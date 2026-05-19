package com.docbase.domain.knowledge.document;

public final class KnowledgeDocumentConstant {

    private KnowledgeDocumentConstant() {
    }

    public static final class Visibility {
        public static final int PUBLIC = 1;
        public static final int DEPT = 2;
        public static final int PRIVATE = 3;

        private Visibility() {
        }
    }

    public static final class Status {
        public static final int DRAFT = 1;
        public static final int PENDING_AUDIT = 2;
        public static final int PUBLISHED = 3;
        public static final int REJECTED = 4;
        public static final int ARCHIVED = 5;

        private Status() {
        }
    }
}
