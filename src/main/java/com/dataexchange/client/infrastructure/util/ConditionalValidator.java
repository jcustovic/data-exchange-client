package com.dataexchange.client.infrastructure.util;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import static org.springframework.util.StringUtils.isEmpty;

public class ConditionalValidator implements ConstraintValidator<Conditional, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConditionalValidator.class);

    private String selected;
    private String[] required;
    private String message;
    private String[] values;
    private boolean exists;

    @Override
    public void initialize(Conditional requiredIfChecked) {
        selected = requiredIfChecked.selected();
        required = requiredIfChecked.required();
        message = requiredIfChecked.message();
        values = requiredIfChecked.values();
        exists = requiredIfChecked.exists();
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {
        Boolean valid = true;
        try {
            Object checkedValue = BeanUtils.getProperty(object, selected);
            if (exists == isExist(checkedValue)) {
                for (String propName : required) {
                    Object requiredValue = BeanUtils.getProperty(object, propName);
                    valid = requiredValue != null && !isEmpty(requiredValue);
                    System.out.println("value: " + "" + requiredValue);
                    if (!valid) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate(message).addPropertyNode(propName).addConstraintViolation();
                    }
                }
            }
        } catch (IllegalAccessException e) {
            LOGGER.error("Accessor method is not available for class : {}, exception : {}", object.getClass().getName(), e);
            e.printStackTrace();
            return false;
        } catch (NoSuchMethodException e) {
            LOGGER.error("Field or method is not present on class : {}, exception : {}", object.getClass().getName(), e);
            e.printStackTrace();
            return false;
        } catch (InvocationTargetException e) {
            LOGGER.error("An exception occurred while accessing class : {}, exception : {}", object.getClass().getName(), e);
            e.printStackTrace();
            return false;
        }
        return valid;
    }

    private boolean isExist(Object checkedValue) {
        return checkedValue != null;
    }
}