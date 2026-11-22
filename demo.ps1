# End-to-end demo of cashapp. Needs the app running:
#   docker compose up -d
#   .\mvnw.cmd spring-boot:run
# Then, in a second terminal:  .\demo.ps1

$baseUrl = "http://localhost:8080"
$runId = Get-Date -Format "HHmmss"   # makes invoice numbers unique, so the demo can run again

function Send-Json($method, $path, $body) {
    $json = $null
    if ($body) { $json = $body | ConvertTo-Json }
    Invoke-RestMethod -Method $method -Uri "$baseUrl$path" -ContentType "application/json" -Body $json
}

Write-Host "`n1) Create a customer" -ForegroundColor Cyan
$customer = Send-Json Post "/customers" @{ name = "Acme Ltd"; email = "ap@acme.example" }
$customer | Format-List

Write-Host "2) Create three invoices for that customer" -ForegroundColor Cyan
$invoiceA = Send-Json Post "/invoices" @{ invoiceNumber = "INV-$runId-A"; customerId = $customer.id; totalAmount = 1000.00; dueDate = "2026-01-15" }
$invoiceB = Send-Json Post "/invoices" @{ invoiceNumber = "INV-$runId-B"; customerId = $customer.id; totalAmount = 2500.00; dueDate = "2026-02-15" }
$invoiceC = Send-Json Post "/invoices" @{ invoiceNumber = "INV-$runId-C"; customerId = $customer.id; totalAmount = 1500.00; dueDate = "2026-03-15" }
$invoiceA, $invoiceB, $invoiceC | Format-Table invoiceNumber, totalAmount, openAmount, dueDate, status

Write-Host "3) Payment of 3000 whose remittance note names invoice B (B is only 2500, so it overpays it)" -ForegroundColor Cyan
$payment = Send-Json Post "/payments" @{ customerId = $customer.id; amount = 3000.00; paymentDate = "2026-03-01"; referenceText = "Payment for $($invoiceB.invoiceNumber)" }
$payment | Format-List id, amount, unappliedAmount, referenceText, status

Write-Host "4) Match it: B is paid by REFERENCE_MATCH, the extra 500 goes to the oldest invoice (A)" -ForegroundColor Cyan
$matchResult = Send-Json Post "/payments/$($payment.id)/match" $null
$matchResult.applications | Format-Table invoiceNumber, amountApplied, matchRule
$matchResult | Format-List paymentId, unappliedAmount, paymentStatus

Write-Host "5) Second payment of 2400, no note: oldest-first pays A and C, 400 stays as credit on account" -ForegroundColor Cyan
$secondPayment = Send-Json Post "/payments" @{ customerId = $customer.id; amount = 2400.00; paymentDate = "2026-03-05" }
$secondMatch = Send-Json Post "/payments/$($secondPayment.id)/match" $null
$secondMatch.applications | Format-Table invoiceNumber, amountApplied, matchRule
$secondMatch | Format-List paymentId, unappliedAmount, paymentStatus

Write-Host "6) Invoices after matching" -ForegroundColor Cyan
Send-Json Get "/invoices?customerId=$($customer.id)" $null | Format-Table invoiceNumber, totalAmount, openAmount, status

Write-Host "7) Matching the first payment again is rejected with 409, because it is fully applied" -ForegroundColor Cyan
try {
    Send-Json Post "/payments/$($payment.id)/match" $null
} catch {
    $errorBody = $_.ErrorDetails.Message
    if (-not $errorBody) {
        # Windows PowerShell 5.1 does not fill ErrorDetails, so read the response body ourselves
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errorBody = $reader.ReadToEnd()
    }
    Write-Host "HTTP $($_.Exception.Response.StatusCode.value__): $errorBody"
}
