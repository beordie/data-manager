package com.tipdm.framework.dmserver.core.scheduling;

/**
 * Created by zhoulong on 2019/4/28.
 */
public class Dependency {

    private String source;

    private String target;

    public Dependency(String source, String target){
        this.source = source;
        this.target = target;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null){
            return false;
        }
        if(obj instanceof Dependency) {
            Dependency comp = (Dependency) obj;
            return this.target.equals(comp.getTarget()) && this.source.equals(comp.getSource());
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return source.hashCode() + target.hashCode();
    }
}
