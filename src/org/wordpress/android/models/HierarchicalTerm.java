package org.wordpress.android.models;

public class HierarchicalTerm {
    private Term term;
    private HierarchicalTerm parent;

    public HierarchicalTerm(Term term) {
        this.term = term;
    }

    public Term getTerm() {
        return this.term;
    }

    public void setParent(HierarchicalTerm parent) {
        this.parent = parent;
    }

    public int getHierarchy() {
        if (parent == null) {
            return 0;
        } else {
            return parent.getHierarchy() + 1;
        }
    }
}
