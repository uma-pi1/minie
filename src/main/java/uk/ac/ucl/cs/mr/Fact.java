package uk.ac.ucl.cs.mr;

/**
 * @author Pasquale Minervini
 */

public class Fact {

    public String subject = null;
    public String predicate = null;
    public String object = null;

    public Fact(String s, String p, String o) {
        this.subject = s;
        this.predicate = p;
        this.object = o;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getPredicate() {
        return predicate;
    }

    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

}
