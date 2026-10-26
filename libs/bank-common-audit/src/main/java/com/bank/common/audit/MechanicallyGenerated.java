package com.bank.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a member expanded from Lombok. JaCoCo ignores an annotation whose simple name contains
 * {@code Generated}, which is how {@code lombok.Generated} stayed out of the coverage floor. Do not
 * put this on a method written by hand. New types do not use it.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
@interface MechanicallyGenerated {}
