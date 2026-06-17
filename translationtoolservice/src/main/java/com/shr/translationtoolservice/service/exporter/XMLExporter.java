package com.shr.translationtoolservice.service.exporter;

import java.io.OutputStream;
import java.util.Collection;

public interface XMLExporter<T> extends Exporter {
    
    public abstract int export(Collection<T> collection,OutputStream outputStream);
}
