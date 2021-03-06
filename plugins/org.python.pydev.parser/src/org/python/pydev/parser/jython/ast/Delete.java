// Autogenerated AST node
package org.python.pydev.parser.jython.ast;
import org.python.pydev.parser.jython.SimpleNode;

public final class Delete extends stmtType {
    public exprType[] targets;

    public Delete(exprType[] targets) {
        this.targets = targets;
    }

    public Delete(exprType[] targets, SimpleNode parent) {
        this(targets);
        this.beginLine = parent.beginLine;
        this.beginColumn = parent.beginColumn;
    }

    public Delete createCopy() {
        exprType[] new0;
        if(this.targets != null){
        new0 = new exprType[this.targets.length];
        for(int i=0;i<this.targets.length;i++){
            new0[i] = (exprType) (this.targets[i] != null? this.targets[i].createCopy():null);
        }
        }else{
            new0 = this.targets;
        }
        Delete temp = new Delete(new0);
        temp.beginLine = this.beginLine;
        temp.beginColumn = this.beginColumn;
        if(this.specialsBefore != null){
            for(Object o:this.specialsBefore){
                if(o instanceof commentType){
                    commentType commentType = (commentType) o;
                    temp.getSpecialsBefore().add(commentType.createCopy());
                }
            }
        }
        if(this.specialsAfter != null){
            for(Object o:this.specialsAfter){
                if(o instanceof commentType){
                    commentType commentType = (commentType) o;
                    temp.getSpecialsAfter().add(commentType.createCopy());
                }
            }
        }
        return temp;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("Delete[");
        sb.append("targets=");
        sb.append(dumpThis(this.targets));
        sb.append("]");
        return sb.toString();
    }

    public Object accept(VisitorIF visitor) throws Exception {
        return visitor.visitDelete(this);
    }

    public void traverse(VisitorIF visitor) throws Exception {
        if (targets != null) {
            for (int i = 0; i < targets.length; i++) {
                if (targets[i] != null){
                    targets[i].accept(visitor);
                }
            }
        }
    }

}
