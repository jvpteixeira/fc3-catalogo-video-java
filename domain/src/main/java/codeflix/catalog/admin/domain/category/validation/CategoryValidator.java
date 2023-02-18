package codeflix.catalog.admin.domain.category.validation;

import codeflix.catalog.admin.domain._share.validation.Error;
import codeflix.catalog.admin.domain._share.validation.ValidationHandler;
import codeflix.catalog.admin.domain._share.validation.Validator;
import codeflix.catalog.admin.domain.category.entity.Category;

public class CategoryValidator extends Validator {

    private static final int NAME_MIN_VALUE = 3;
    private static final int NAME_MAX_VALUE = 255;
    private final Category category;

    public CategoryValidator(final Category aCategory, final ValidationHandler aHandler) {
        super(aHandler);
        this.category = aCategory;
    }

    @Override
    public void validate() {
        this.checkNameConstraints();
    }

    private void checkNameConstraints() {
        final String name = this.category.getName();
        if (name == null) { // Notification Pattern
            this.validationHandler().append(new Error("'name' should not be null"));
            return;
        }

        if (name.isBlank()) {
            this.validationHandler().append(new Error("'name' should not be empty"));
            return;
        }

        final int length = name.trim().length();
        if (length < NAME_MIN_VALUE || length > NAME_MAX_VALUE)
            this.validationHandler().append(new Error("'name' must be between 3 and 255 characters"));
    }
}
