package com.aztu.support.service;

import java.util.List;

/**
 * Content model for the single reusable HTML email template
 * ({@code templates/email/generic.html}).
 */
public record EmailContent(
        String heading,
        String greeting,
        List<String> paragraphs,
        String ctaLabel,
        String ctaUrl,
        String footerNote) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String heading;
        private String greeting;
        private List<String> paragraphs = List.of();
        private String ctaLabel;
        private String ctaUrl;
        private String footerNote;

        public Builder heading(String v) { this.heading = v; return this; }
        public Builder greeting(String v) { this.greeting = v; return this; }
        public Builder paragraphs(List<String> v) { this.paragraphs = v; return this; }
        public Builder cta(String label, String url) { this.ctaLabel = label; this.ctaUrl = url; return this; }
        public Builder footerNote(String v) { this.footerNote = v; return this; }

        public EmailContent build() {
            return new EmailContent(heading, greeting, paragraphs, ctaLabel, ctaUrl, footerNote);
        }
    }
}
