Param(
  [string]$BaseUrl = "http://localhost:8081",
  [string]$Tenant = "local"
)

$ErrorActionPreference = 'Continue'
$ts = Get-Date -Format 'yyyyMMddHHmmss'
$logPath = Join-Path $env:TEMP "api-full-$ts.log"

function Write-Result {
  param(
    [string]$Name,
    [string]$Method,
    [string]$Path,
    [int]$StatusCode,
    [bool]$Success,
    [string]$Snippet,
    [int]$Ms
  )
  $status = if ($Success) { 'OK' } else { 'FAIL' }
  $line = "[$status] $Name ($Method $Path) - $StatusCode in ${Ms}ms"
  Write-Host $line
  Add-Content -Path $logPath -Value $line
  if ($Snippet) {
    $snip = ($Snippet.Substring(0, [Math]::Min($Snippet.Length, 250)))
    Write-Host "  -> " $snip
    Add-Content -Path $logPath -Value ("  -> " + $snip)
  }
}

function Invoke-Api {
  param(
    [string]$Name,
    [string]$Method,
    [string]$Path,
    [hashtable]$Body = $null,
    [hashtable]$Headers = $null,
    $Session
  )
  $sw = [Diagnostics.Stopwatch]::StartNew()
  $uri = "$BaseUrl$Path"
  try {
    $params = @{ Uri = $uri; Method = $Method; WebSession = $Session }
    if ($Headers) { $params.Headers = $Headers }
    if ($Method -in @('POST','PUT','PATCH')) {
      $json = if ($Body -ne $null) { ($Body | ConvertTo-Json -Depth 10) } else { '{}' }
      $params.Body = $json
      if (-not $params.ContainsKey('Headers')) { $params.Headers = @{} }
      $params.ContentType = 'application/json'
    }
    $resp = Invoke-WebRequest @params
    $sw.Stop()
    Write-Result -Name $Name -Method $Method -Path $Path -StatusCode $resp.StatusCode -Success:$true -Snippet $resp.Content -Ms $sw.ElapsedMilliseconds
    return $resp
  } catch {
    $sw.Stop()
    $code = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { -1 }
    $content = try {
      if ($_.Exception.Response) {
        $reader = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.ReadToEnd()
      } else { $_.Exception.Message }
    } catch { $_.Exception.Message }
    Write-Result -Name $Name -Method $Method -Path $Path -StatusCode $code -Success:$false -Snippet $content -Ms $sw.ElapsedMilliseconds
    return $null
  }
}

function Invoke-ApiForm {
  param(
    [string]$Name,
    [string]$Path,
    [hashtable]$Form,
    [hashtable]$Headers = $null,
    $Session
  )
  $sw = [Diagnostics.Stopwatch]::StartNew()
  $uri = "$BaseUrl$Path"
  try {
    $resp = Invoke-WebRequest -Uri $uri -Method 'POST' -WebSession $Session -Headers $Headers -Form $Form
    $sw.Stop()
    Write-Result -Name $Name -Method 'POST' -Path $Path -StatusCode $resp.StatusCode -Success:$true -Snippet $resp.Content -Ms $sw.ElapsedMilliseconds
    return $resp
  } catch {
    $sw.Stop()
    $code = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { -1 }
    $content = try {
      if ($_.Exception.Response) {
        $reader = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.ReadToEnd()
      } else { $_.Exception.Message }
    } catch { $_.Exception.Message }
    Write-Result -Name $Name -Method 'POST' -Path $Path -StatusCode $code -Success:$false -Snippet $content -Ms $sw.ElapsedMilliseconds
    return $null
  }
}

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Write-Host "Running FULL API tests against $BaseUrl (tenant '$Tenant')"
Write-Host "Logging to: $logPath"

$ok = 0; $fail = 0

# Actuator and docs
$resp = Invoke-Api -Name 'Actuator health' -Method 'GET' -Path '/actuator/health' -Session $session; if ($resp) { $ok++ } else { $fail++ }
$resp = Invoke-Api -Name 'Swagger api-docs' -Method 'GET' -Path '/v3/api-docs' -Session $session; if ($resp) { $ok++ } else { $fail++ }

# Storefront register/login/profile
$sfEmail = "sf_$ts@example.com"
$sfAuthHeaders = @{ 'X-Tenant-Domain' = $Tenant }
$resp = Invoke-Api -Name 'SF register' -Method 'POST' -Path '/api/storefront/auth/register' -Headers $sfAuthHeaders -Body @{ name='SF User'; email=$sfEmail; password='Passw0rd!' } -Session $session; if ($resp) { $ok++ } else { $fail++ }
$sfLogin = Invoke-Api -Name 'SF login' -Method 'POST' -Path '/api/storefront/auth/login' -Headers $sfAuthHeaders -Body @{ email=$sfEmail; password='Passw0rd!' } -Session $session; if ($sfLogin) { $ok++ } else { $fail++ }
$sfToken = $null
try { $sfToken = (($sfLogin.Content | ConvertFrom-Json).token) } catch {}
$sfHeaders = if ($sfToken) { @{ Authorization = "Bearer $sfToken"; 'X-Tenant-Domain'=$Tenant } } else { @{ 'X-Tenant-Domain'=$Tenant } }
$resp = Invoke-Api -Name 'SF profile' -Method 'GET' -Path '/api/storefront/auth/profile' -Headers $sfHeaders -Session $session; if ($resp) { $ok++ } else { $fail++ }

# Storefront catalog
$sfProducts = Invoke-Api -Name 'SF products' -Method 'GET' -Path '/api/storefront/products' -Headers $sfHeaders -Session $session; if ($sfProducts) { $ok++ } else { $fail++ }
$sfCategories = Invoke-Api -Name 'SF categories' -Method 'GET' -Path '/api/storefront/categories' -Headers $sfHeaders -Session $session; if ($sfCategories) { $ok++ } else { $fail++ }

# Storefront checkout (attempt with minimal payload)
$firstProd = $null
try { $firstProd = ($sfProducts.Content | ConvertFrom-Json)[0] } catch {}
# Force a JSON array for items even when single-element
$items = New-Object System.Collections.ArrayList
if ($firstProd) {
  [void]$items.Add(@{ id=$firstProd.id; name=$firstProd.name; price=$firstProd.price; quantity=1 })
} else {
  [void]$items.Add(@{ id='p1'; name='Demo'; price=100; quantity=1 })
}
$payable = try { [double]($items[0].price) } catch { 100 }
$checkoutBody = @{ items=$items; totals=@{ payable=$payable }; paymentMethod='cod'; address=@{ line1='123'; city='BLR' } }
$resp = Invoke-Api -Name 'SF checkout' -Method 'POST' -Path '/api/storefront/checkout' -Headers $sfHeaders -Body $checkoutBody -Session $session; if ($resp) { $ok++ } else { $fail++ }

# Storefront orders/bookings
$resp = Invoke-Api -Name 'SF orders' -Method 'GET' -Path '/api/storefront/orders' -Headers $sfHeaders -Session $session; if ($resp) { $ok++ } else { $fail++ }
$resp = Invoke-Api -Name 'SF bookings' -Method 'GET' -Path '/api/storefront/bookings' -Headers $sfHeaders -Session $session; if ($resp) { $ok++ } else { $fail++ }

# Storefront payments
$resp = Invoke-Api -Name 'SF payments create-order' -Method 'POST' -Path '/api/storefront/payments/create-order' -Headers $sfHeaders -Body @{ amount=100; currency='INR' } -Session $session; if ($resp) { $ok++ } else { $fail++ }
$resp = Invoke-Api -Name 'SF payments verify' -Method 'POST' -Path '/api/storefront/payments/verify' -Headers $sfHeaders -Body @{ orderId='order_123'; paymentId='pay_123'; signature='sig_abc' } -Session $session; if ($resp) { $ok++ } else { $fail++ }

# Seller register/login
$sellerEmail = "seller_$ts@example.com"
$resp = Invoke-Api -Name 'Seller register' -Method 'POST' -Path '/api/auth/seller/register' -Body @{ name='Seller Test'; email=$sellerEmail; password='Passw0rd!' } -Session $session; if ($resp) { $ok++ } else { $fail++ }
$sellerLogin = Invoke-Api -Name 'Seller login' -Method 'POST' -Path '/api/auth/seller/login' -Body @{ email=$sellerEmail; password='Passw0rd!' } -Session $session; if ($sellerLogin) { $ok++ } else { $fail++ }
$sellerToken = $null
try { $sellerToken = (($sellerLogin.Content | ConvertFrom-Json).token) } catch {}
$sellerHeaders = if ($sellerToken) { @{ Authorization = "Bearer $sellerToken"; 'X-Tenant-Domain'=$Tenant } } else { @{ 'X-Tenant-Domain'=$Tenant } }

# Seller store create/update/list
$storeCreate = Invoke-Api -Name 'Seller create store' -Method 'POST' -Path '/api/seller/stores' -Headers $sellerHeaders -Body @{ name='Test Store'; city='City'; area='Area'; category='General' } -Session $session; if ($storeCreate) { $ok++ } else { $fail++ }
$storeId = $null
try { $storeId = (($storeCreate.Content | ConvertFrom-Json).id) } catch {}
if ($storeId) {
  $resp = Invoke-Api -Name 'Seller update store' -Method 'PATCH' -Path "/api/seller/stores/$storeId" -Headers $sellerHeaders -Body @{ name='Updated Store'; area='Updated Area'; city='Updated City'; category='Updated Category' } -Session $session; if ($resp) { $ok++ } else { $fail++ }
}
$resp = Invoke-Api -Name 'Seller list stores' -Method 'GET' -Path '/api/seller/stores?page=0&limit=20' -Headers $sellerHeaders -Session $session; if ($resp) { $ok++ } else { $fail++ }

# Seller products list/create/update/inventory/delete/upload image
if ($storeId) {
  $resp = Invoke-Api -Name 'Seller products list' -Method 'GET' -Path "/api/seller/stores/$storeId/products" -Headers $sellerHeaders -Session $session; if ($resp) { $ok++ } else { $fail++ }
  $prodCreate = Invoke-Api -Name 'Seller create product' -Method 'POST' -Path "/api/seller/stores/$storeId/products" -Headers $sellerHeaders -Body @{ name='Product Name'; price=299; category='General'; currency='INR'; description='Desc'; image='image.jpg'; sku='SKU123'; stock=10; active=$true } -Session $session; if ($prodCreate) { $ok++ } else { $fail++ }
  $productId = $null
  try { $productId = (($prodCreate.Content | ConvertFrom-Json).id) } catch {}
  if ($productId) {
    $resp = Invoke-Api -Name 'Seller update product' -Method 'PATCH' -Path "/api/seller/products/$productId" -Headers $sellerHeaders -Body @{ price=349; active=$true } -Session $session; if ($resp) { $ok++ } else { $fail++ }
    $resp = Invoke-Api -Name 'Seller adjust inventory' -Method 'PATCH' -Path "/api/seller/products/$productId/inventory" -Headers $sellerHeaders -Body @{ stockDelta=5; price=299 } -Session $session; if ($resp) { $ok++ } else { $fail++ }
    # Upload image via multipart form
    $tmp = Join-Path $env:TEMP "bs_image_$ts.jpg"
    Set-Content -Path $tmp -Value 'fake image content'
    $form = @{ file = (Get-Item $tmp) }
    $resp = Invoke-ApiForm -Name 'Seller upload image' -Path "/api/seller/products/$productId/images" -Headers $sellerHeaders -Form $form -Session $session; if ($resp) { $ok++ } else { $fail++ }
    # Delete product (archive=true)
    $resp = Invoke-Api -Name 'Seller delete product' -Method 'DELETE' -Path "/api/seller/products/$productId?archive=true" -Headers $sellerHeaders -Session $session; if ($resp) { $ok++ } else { $fail++ }
  }
}

# Seller orders/list/detail/status/refund
$ordersList = Invoke-Api -Name 'Seller orders list' -Method 'GET' -Path '/api/seller/orders?page=0&limit=25' -Headers $sellerHeaders -Session $session; if ($ordersList) { $ok++ } else { $fail++ }
$firstOrderId = $null
try { $firstOrderId = (($ordersList.Content | ConvertFrom-Json)[0].id) } catch {}
if ($firstOrderId) {
  $resp = Invoke-Api -Name 'Seller order detail' -Method 'GET' -Path "/api/seller/orders/$firstOrderId" -Headers $sellerHeaders -Session $session; if ($resp) { $ok++ } else { $fail++ }
  $resp = Invoke-Api -Name 'Seller order status update' -Method 'PATCH' -Path "/api/seller/orders/$firstOrderId/status" -Headers $sellerHeaders -Body @{ status='shipped'; notes='Packed and ready' } -Session $session; if ($resp) { $ok++ } else { $fail++ }
  $resp = Invoke-Api -Name 'Seller create refund' -Method 'POST' -Path "/api/seller/orders/$firstOrderId/refunds" -Headers $sellerHeaders -Body @{ amount=100; reason='Customer returned item' } -Session $session; if ($resp) { $ok++ } else { $fail++ }
}

# Seller bookings list/status
$bookingsList = Invoke-Api -Name 'Seller bookings list' -Method 'GET' -Path '/api/seller/bookings' -Headers $sellerHeaders -Session $session; if ($bookingsList) { $ok++ } else { $fail++ }
$firstBookingId = $null
try { $firstBookingId = (($bookingsList.Content | ConvertFrom-Json)[0].id) } catch {}
if ($firstBookingId) {
  $resp = Invoke-Api -Name 'Seller booking status update' -Method 'PATCH' -Path "/api/seller/bookings/$firstBookingId/status" -Headers $sellerHeaders -Body @{ status='confirmed' } -Session $session; if ($resp) { $ok++ } else { $fail++ }
}

# Seller analytics, payouts, announcements
$resp = Invoke-Api -Name 'Seller analytics overview' -Method 'GET' -Path '/api/seller/analytics/overview' -Headers $sellerHeaders -Session $session; if ($resp) { $ok++ } else { $fail++ }
$resp = Invoke-Api -Name 'Seller payouts list' -Method 'GET' -Path '/api/seller/payouts' -Headers $sellerHeaders -Session $session; if ($resp) { $ok++ } else { $fail++ }
if ($storeId) {
  $resp = Invoke-Api -Name 'Seller post announcement' -Method 'POST' -Path '/api/seller/announcements' -Headers $sellerHeaders -Body @{ storeId=$storeId; message='Store closed on Jan 26' } -Session $session; if ($resp) { $ok++ } else { $fail++ }
}

# Platform products
$resp = Invoke-Api -Name 'Platform products list' -Method 'GET' -Path '/api/platform/products' -Session $session; if ($resp) { $ok++ } else { $fail++ }
$resp = Invoke-Api -Name 'Platform product create' -Method 'POST' -Path '/api/platform/products' -Headers $sellerHeaders -Body @{ name='Platform Item'; price=299; description='Desc'; category='General'; storeId=$storeId } -Session $session; if ($resp) { $ok++ } else { $fail++ }

# Legacy storefront flows
$resp = Invoke-Api -Name 'Legacy products' -Method 'GET' -Path '/store/products' -Session $session; if ($resp) { $ok++ } else { $fail++ }
$resp = Invoke-Api -Name 'Legacy categories' -Method 'GET' -Path '/store/categories' -Session $session; if ($resp) { $ok++ } else { $fail++ }
$resp = Invoke-Api -Name 'Legacy orders list' -Method 'GET' -Path '/store/orders' -Headers $sfHeaders -Session $session; if ($resp) { $ok++ } else { $fail++ }
# Try order detail only if any order exists
$legacyOrders = $null
try { $legacyOrders = ($resp.Content | ConvertFrom-Json) } catch {}
if ($legacyOrders -and $legacyOrders.Count -gt 0) {
  $legacyOrderId = $legacyOrders[0].id
  $resp = Invoke-Api -Name 'Legacy order detail' -Method 'GET' -Path "/store/orders/$legacyOrderId" -Headers $sfHeaders -Session $session; if ($resp) { $ok++ } else { $fail++ }
}

# Legacy checkout and payments
$address = @{ line1='Test Address'; city='City'; area='Area'; pin='000000' }
$resp = Invoke-Api -Name 'Legacy checkout' -Method 'POST' -Path '/store/checkout' -Headers $sfHeaders -Body @{ items=@(@{ id='p1'; name='Prod'; price=100; quantity=1 }); totals=@{ payable=100 }; address=$address; paymentMethod='cod' } -Session $session; if ($resp) { $ok++ } else { $fail++ }
$resp = Invoke-Api -Name 'Legacy payments initiate' -Method 'POST' -Path '/store/payments/initiate' -Headers $sfHeaders -Body @{ amount=100; currency='INR' } -Session $session; if ($resp) { $ok++ } else { $fail++ }
$resp = Invoke-Api -Name 'Legacy payments verify' -Method 'POST' -Path '/store/payments/verify' -Headers $sfHeaders -Body @{ razorpay_order_id='order_abc'; razorpay_payment_id='pay_def'; razorpay_signature='sig_xyz' } -Session $session; if ($resp) { $ok++ } else { $fail++ }
$resp = Invoke-Api -Name 'Legacy payments webhook' -Method 'POST' -Path '/store/payments/webhook' -Body @{ event='payment.captured'; payload=@{ id='pay_123' } } -Session $session; if ($resp) { $ok++ } else { $fail++ }

Write-Host ""; Write-Host "FULL Test Summary: $ok OK, $fail FAIL"
exit 0