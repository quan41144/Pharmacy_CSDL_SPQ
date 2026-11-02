package com.pharmacy.medicare.dto.invoice;

public class ConfirmPaymentResponse {

    private boolean success;
    private String resultState; // "true" or "fail"
    private String message;
    private InvoiceResponse invoice;

    public ConfirmPaymentResponse() {
    }

    public ConfirmPaymentResponse(boolean success, String resultState, String message, InvoiceResponse invoice) {
        this.success = success;
        this.resultState = resultState;
        this.message = message;
        this.invoice = invoice;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getResultState() {
        return resultState;
    }

    public void setResultState(String resultState) {
        this.resultState = resultState;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public InvoiceResponse getInvoice() {
        return invoice;
    }

    public void setInvoice(InvoiceResponse invoice) {
        this.invoice = invoice;
    }
}
