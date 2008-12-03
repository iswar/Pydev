// Autogenerated AST node
package org.python.pydev.parser.jython.ast;
import org.python.pydev.parser.jython.SimpleNode;
import java.io.DataOutputStream;
import java.io.IOException;

public class ExtSlice extends sliceType {
    public sliceType[] dims;

    public ExtSlice(sliceType[] dims) {
        this.dims = dims;
    }

    public ExtSlice(sliceType[] dims, SimpleNode parent) {
        this(dims);
        this.beginLine = parent.beginLine;
        this.beginColumn = parent.beginColumn;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("ExtSlice[");
        sb.append("dims=");
        sb.append(dumpThis(this.dims));
        sb.append("]");
        return sb.toString();
    }

    public void pickle(DataOutputStream ostream) throws IOException {
        pickleThis(55, ostream);
        pickleThis(this.dims, ostream);
    }

    public Object accept(VisitorIF visitor) throws Exception {
        return visitor.visitExtSlice(this);
    }

    public void traverse(VisitorIF visitor) throws Exception {
        if (dims != null) {
            for (int i = 0; i < dims.length; i++) {
                if (dims[i] != null)
                    dims[i].accept(visitor);
            }
        }
    }

}
