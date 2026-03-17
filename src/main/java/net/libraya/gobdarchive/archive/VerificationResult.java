package net.libraya.gobdarchive.archive;

import java.util.ArrayList;
import java.util.List;

public class VerificationResult {

    private boolean success;
    private List<String> errors = new ArrayList<>();

    public VerificationResult() {
        this.success = true;
    }

    public boolean isSuccess() {
        return success && errors.isEmpty();
    }

    public void addError(String error) {
        this.success = false;
        this.errors.add(error);
    }

    public List<String> getErrors() {
        return errors;
    }
}