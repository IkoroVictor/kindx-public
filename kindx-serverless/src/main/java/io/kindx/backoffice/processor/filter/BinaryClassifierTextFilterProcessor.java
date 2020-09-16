package io.kindx.backoffice.processor.filter;

import org.apache.commons.lang3.StringUtils;

public class BinaryClassifierTextFilterProcessor implements TextFilterProcessor {

    private String textSignature;
    private boolean useTextSignature =  false;

    public BinaryClassifierTextFilterProcessor() {
    }

    public BinaryClassifierTextFilterProcessor(String textSignature) {
        this.textSignature = textSignature;
        useTextSignature = StringUtils.isNotBlank(textSignature);
    }


    @Override
    public boolean query(String text) {
        if (useTextSignature && text.contains(textSignature)){
            return true;
        }
        //TODO: Use sagemaker for binary classification
        return false;
    }
}
