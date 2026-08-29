package com.oms.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InputSanitizerTest {


    @Test
    void sanitize_shouldReturnNullForNullInput() {
        assertThat(InputSanitizer.sanitize(null)).isNull();
    }

    @Test
    void sanitize_shouldReturnBlankForBlankInput() {
        assertThat(InputSanitizer.sanitize("   ")).isEqualTo("   ");
    }

    @Test
    void sanitize_shouldPreserveNormalText() {
        assertThat(InputSanitizer.sanitize("Kaos Polos Merah")).isEqualTo("Kaos Polos Merah");
    }

    @Test
    void sanitize_shouldStripHtmlTags() {
        // sanitize() strips tags first, then encodes remaining content
        assertThat(InputSanitizer.sanitize("<b>Bold</b> text"))
                .isEqualTo("Bold text");
    }

    @Test
    void sanitize_shouldEncodeHtmlEntities() {
        assertThat(InputSanitizer.sanitize("Tom & Jerry"))
                .isEqualTo("Tom &amp; Jerry");
    }

    @Test
    void sanitize_shouldStripScriptTags() {
        String result = InputSanitizer.sanitize("<script>alert('xss')</script>");
        assertThat(result).doesNotContain("<script");
        assertThat(result).doesNotContain("</script>");
    }

    @Test
    void sanitize_shouldStripEventHandlers() {
        String result = InputSanitizer.sanitize("<img src=x onerror=alert(1)>");
        assertThat(result).doesNotContain("<img");
    }

    @Test
    void sanitize_shouldTruncateLongInput() {
        String result = InputSanitizer.sanitize("a".repeat(1500));
        assertThat(result.length()).isLessThanOrEqualTo(1000);
    }

    @Test
    void sanitize_shouldEncodeQuotes() {
        assertThat(InputSanitizer.sanitize("He said \"hello\""))
                .isEqualTo("He said &quot;hello&quot;");
    }


    @Test
    void stripHtmlTags_shouldReturnNullForNullInput() {
        assertThat(InputSanitizer.stripHtmlTags(null)).isNull();
    }

    @Test
    void stripHtmlTags_shouldRemoveAllHtmlTags() {
        assertThat(InputSanitizer.stripHtmlTags("<div class='test'>content</div>"))
                .isEqualTo("content");
    }

    @Test
    void stripHtmlTags_shouldPreservePlainText() {
        assertThat(InputSanitizer.stripHtmlTags("plain text"))
                .isEqualTo("plain text");
    }


    @Test
    void encodeHtmlEntities_shouldReturnNullForNullInput() {
        assertThat(InputSanitizer.encodeHtmlEntities(null)).isNull();
    }

    @Test
    void encodeHtmlEntities_shouldEncodeAllSpecialChars() {
        assertThat(InputSanitizer.encodeHtmlEntities("<script>&\"'</script>"))
                .isEqualTo("&lt;script&gt;&amp;&quot;&#x27;&lt;/script&gt;");
    }


    @Test
    void containsUnsafePatterns_shouldReturnFalseForSafeInput() {
        assertThat(InputSanitizer.containsUnsafePatterns(null)).isFalse();
        assertThat(InputSanitizer.containsUnsafePatterns("")).isFalse();
        assertThat(InputSanitizer.containsUnsafePatterns("Kaos Polos")).isFalse();
    }

    @Test
    void containsUnsafePatterns_shouldDetectXssPatterns() {
        assertThat(InputSanitizer.containsUnsafePatterns("javascript:alert(1)")).isTrue();
        assertThat(InputSanitizer.containsUnsafePatterns("<script>alert(1)</script>")).isTrue();
        assertThat(InputSanitizer.containsUnsafePatterns("<iframe src=evil.com>")).isTrue();
        assertThat(InputSanitizer.containsUnsafePatterns("onclick=run()")).isTrue();
        assertThat(InputSanitizer.containsUnsafePatterns("onload=hack()")).isTrue();
        assertThat(InputSanitizer.containsUnsafePatterns("data:text/html,xss")).isTrue();
    }

    @Test
    void containsUnsafePatterns_shouldBeCaseInsensitive() {
        assertThat(InputSanitizer.containsUnsafePatterns("<SCRIPT>alert(1)</SCRIPT>")).isTrue();
        assertThat(InputSanitizer.containsUnsafePatterns("OnErRoR=alert(1)")).isTrue();
    }
}
