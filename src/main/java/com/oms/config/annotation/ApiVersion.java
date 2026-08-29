package com.oms.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to mark API version for a controller.
 * <p>
 * Usage:
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/products")
 * @ApiVersion("v1")
 * public class ProductV1Controller { ... }
 * }</pre>
 * <p>
 * The version information can be used for:
 * <ul>
 *   <li>Documentation (Swagger/OpenAPI grouping)</li>
 *   <li>Runtime version resolution via {@link com.oms.config.versioning.VersioningResolver}</li>
 *   <li>Deprecation header management</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiVersion {

    /**
     * The API version identifier (e.g., "v1", "v2").
     */
    String value();
}