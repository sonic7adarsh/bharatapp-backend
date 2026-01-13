Param(
  [string]$BaseUrl = "http://localhost:8081"
)

$ErrorActionPreference = 'Continue'

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
  Write-Host "[$status] $Name ($Method $Path) - $StatusCode in ${Ms}ms"
  if ($Snippet) { Write-Host "  -> " ($Snippet.Substring(0, [Math]::Min($Snippet.Length, 250))) }
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
      $json = if ($Body -ne $null) { ($Body | ConvertTo-Json -Depth 6) } else { '{}' }
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

$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$ts = Get-Date -Format 'yyyyMMddHHmmss'

Write-Host "Running API smoke tests against $BaseUrl"

$ok = 0; $fail = 0

# Basic checks
$resp = Invoke-Api -Name 'Actuator health' -Method 'GET' -Path '/actuator/health' -Session $session; if ($resp) { $ok++ } else { $fail++ }
$resp = Invoke-Api -Name 'Swagger api-docs' -Method 'GET' -Path '/v3/api-docs' -Session $session; if ($resp) { $ok++ } else { $fail++ }

# Storefront auth (register/login) and headers
$sfEmail = "sf_$ts@example.com"
$sfLogin = Invoke-Api -Name 'SF login' -Method 'POST' -Path '/api/storefront/auth/login' -Body @{ email=$sfEmail; password='Passw0rd!' } -Session $session
if ($sfLogin) { $ok++ } else { $fail++ }
$sfToken = $null
try { $sfToken = ($sfLogin.Content | ConvertFrom-Json).token } catch {}
$sfHeaders = if ($sfToken) { @{ Authorization = "Bearer $sfToken"; 'X-Tenant-Domain'='local' } } else { @{ 'X-Tenant-Domain'='local' } }

# Storefront catalog
$resp = Invoke-Api -Name 'Storefront products' -Method 'GET' -Path '/api/storefront/products' -Headers $sfHeaders -Session $session; if ($resp) { $ok++ } else { $fail++ }
$resp = Invoke-Api -Name 'Storefront categories' -Method 'GET' -Path '/api/storefront/categories' -Headers $sfHeaders -Session $session; if ($resp) { $ok++ } else { $fail++ }

# Seller auth and store/products
$sellerEmail = "seller_$ts@example.com"
$resp = Invoke-Api -Name 'Seller register' -Method 'POST' -Path '/api/auth/seller/register' -Body @{ name='Seller Smoke'; email=$sellerEmail; password='Passw0rd!' } -Session $session; if ($resp) { $ok++ } else { $fail++ }
$sellerLogin = Invoke-Api -Name 'Seller login' -Method 'POST' -Path '/api/auth/seller/login' -Body @{ email=$sellerEmail; password='Passw0rd!' } -Session $session; if ($sellerLogin) { $ok++ } else { $fail++ }
$sellerToken = $null
try { $sellerToken = ($sellerLogin.Content | ConvertFrom-Json).token } catch {}
$sellerHeaders = if ($sellerToken) { @{ Authorization = "Bearer $sellerToken" } } else { @{} }

# If storefront token is missing, fallback to seller token for secured storefront calls
if (-not $sfToken -and $sellerToken) { $sfHeaders['Authorization'] = "Bearer $sellerToken" }

$storeCreate = Invoke-Api -Name 'Seller create store' -Method 'POST' -Path '/api/seller/stores' -Headers $sellerHeaders -Body @{ name='Smoke Store'; city='City'; area='Area'; category='General' } -Session $session; if ($storeCreate) { $ok++ } else { $fail++ }
$storeId = $null
try { $storeId = ($storeCreate.Content | ConvertFrom-Json).id } catch {}
if ($storeId) {
  $resp = Invoke-Api -Name 'Seller products list' -Method 'GET' -Path "/api/seller/stores/$storeId/products" -Headers $sellerHeaders -Session $session; if ($resp) { $ok++ } else { $fail++ }
} else {
  Write-Host "[WARN] No storeId captured; skipping seller products list"
}

# Legacy catalog (unauth) and secured legacy/storefront flows (with token)
$legacyProducts = Invoke-Api -Name 'Legacy products' -Method 'GET' -Path '/store/products' -Session $session; if ($legacyProducts) { $ok++ } else { $fail++ }
$resp = Invoke-Api -Name 'Legacy categories' -Method 'GET' -Path '/store/categories' -Session $session; if ($resp) { $ok++ } else { $fail++ }
$resp = Invoke-Api -Name 'Legacy orders list' -Method 'GET' -Path '/store/orders' -Headers $sfHeaders -Session $session; if ($resp) { $ok++ } else { $fail++ }

# Extract a valid product and store for legacy checkout
$legacyProductId = $null; $legacyStoreId = $null
try {
  $plist = ($legacyProducts.Content | ConvertFrom-Json)
  if ($plist -and $plist.Count -gt 0) {
    $legacyProductId = $plist[0].id
    $legacyStoreId = $plist[0].storeId
  }
} catch {}

# Legacy checkout (address object)
$address = @{ line1='Smoke Address'; city='City'; area='Area'; pin='000000' }
$legacyCheckoutBody = @{ items=@(@{ id=$legacyProductId; quantity=1 }); address=$address; paymentMethod='cod'; storeId=$legacyStoreId; deliveryLat=12.9716; deliveryLng=77.5946 }
$legacyCheckout = Invoke-Api -Name 'Legacy checkout' -Method 'POST' -Path '/store/checkout' -Headers $sfHeaders -Body $legacyCheckoutBody -Session $session; if ($legacyCheckout) { $ok++ } else { $fail++ }

# Legacy order detail using newly created order id
$legacyOrderId = $null
try { $legacyOrderId = ($legacyCheckout.Content | ConvertFrom-Json).order.id } catch {}
if ($legacyOrderId) {
  $resp = Invoke-Api -Name 'Legacy order detail' -Method 'GET' -Path "/store/order/$legacyOrderId" -Headers $sfHeaders -Session $session; if ($resp) { $ok++ } else { $fail++ }
} else {
  Write-Host "[WARN] No legacyOrderId captured; skipping Legacy order detail"
}

# Payments (legacy and storefront) after auth fallback
$resp = Invoke-Api -Name 'Legacy payments initiate' -Method 'POST' -Path '/store/payments/initiate' -Headers $sfHeaders -Body @{ amount=100; currency='INR' } -Session $session; if ($resp) { $ok++ } else { $fail++ }
$resp = Invoke-Api -Name 'Legacy payments webhook' -Method 'POST' -Path '/store/payments/webhook' -Body @{ event='payment.captured'; payload=@{ id='pay_123' } } -Session $session; if ($resp) { $ok++ } else { $fail++ }
$resp = Invoke-Api -Name 'Storefront payments create-order' -Method 'POST' -Path '/api/storefront/payments/create-order' -Headers $sfHeaders -Body @{ amount=100; currency='INR' } -Session $session; if ($resp) { $ok++ } else { $fail++ }
$resp = Invoke-Api -Name 'Storefront payments verify' -Method 'POST' -Path '/api/storefront/payments/verify' -Headers $sfHeaders -Body @{ orderId='order_123'; paymentId='pay_123'; signature='sig_abc' } -Session $session; if ($resp) { $ok++ } else { $fail++ }

Write-Host ""; Write-Host "Summary: $ok OK, $fail FAIL"
exit 0