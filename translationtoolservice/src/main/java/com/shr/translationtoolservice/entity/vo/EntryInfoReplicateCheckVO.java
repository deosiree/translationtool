package com.shr.translationtoolservice.entity.vo;

import java.util.List;

import com.shr.translationtoolservice.entity.EntryInfoEntity;

public class EntryInfoReplicateCheckVO{

    public String entry;

    public String productID;

    public String tag;

    public String comment;

    public String abbr;

    public String importType;

    public String entrySource;

    public String diFileName;

    public String maxEntryVersion;

    public List<String> entryInfoIDsForMaxEntryVersion;

    private EntryInfoReplicateCheckVO(){}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entry == null) ? 0 : entry.hashCode());
        result = prime * result + ((productID == null) ? 0 : productID.hashCode());
        result = prime * result + ((tag == null) ? 0 : tag.hashCode());
        result = prime * result + ((comment == null) ? 0 : comment.hashCode());
        result = prime * result + ((abbr == null) ? 0 : abbr.hashCode());
        result = prime * result + ((importType == null) ? 0 : importType.hashCode());
        result = prime * result + ((entrySource == null) ? 0 : entrySource.hashCode());
        result = prime * result + ((diFileName == null) ? 0 : diFileName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EntryInfoReplicateCheckVO other = (EntryInfoReplicateCheckVO) obj;
        if (entry == null) {
            if (other.entry != null)
                return false;
        } else if (!entry.equals(other.entry))
            return false;
        if (productID == null) {
            if (other.productID != null)
                return false;
        } else if (!productID.equals(other.productID))
            return false;
        if (tag == null) {
            if (other.tag != null)
                return false;
        } else if (!tag.equals(other.tag))
            return false;
        if (comment == null) {
            if (other.comment != null)
                return false;
        } else if (!comment.equals(other.comment))
            return false;
        if (abbr == null) {
            if (other.abbr != null)
                return false;
        } else if (!abbr.equals(other.abbr))
            return false;
        if (importType == null) {
            if (other.importType != null)
                return false;
        } else if (!importType.equals(other.importType))
            return false;
        if (entrySource == null) {
            if (other.entrySource != null)
                return false;
        } else if (!entrySource.equals(other.entrySource))
            return false;
        if (diFileName == null) {
            if (other.diFileName != null)
                return false;
        } else if (!diFileName.equals(other.diFileName))
            return false;
        return true;
    }

    public String getEntry() {
        return entry;
    }

    public void setEntry(String entry) {
        this.entry = entry;
    }

    public String getProductID() {
        return productID;
    }

    public void setProductID(String productID) {
        this.productID = productID;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAbbr() {
        return abbr;
    }

    public void setAbbr(String abbr) {
        this.abbr = abbr;
    }

    public String getImportType() {
        return importType;
    }

    public void setImportType(String importType) {
        this.importType = importType;
    }

    public String getEntrySource() {
        return entrySource;
    }

    public void setEntrySource(String entrySource) {
        this.entrySource = entrySource;
    }

    public String getDiFileName() {
        return diFileName;
    }

    public void setDiFileName(String diFileName) {
        this.diFileName = diFileName;
    }
    public String getMaxEntryVersion() {
        return maxEntryVersion;
    }

    public void setMaxEntryVersion(String maxEntryVersion) {
        this.maxEntryVersion = maxEntryVersion;
    }
    public List<String> getEntryInfoIDsForMaxEntryVersion() {
        return entryInfoIDsForMaxEntryVersion;
    }

    public static EntryInfoReplicateCheckVO convertFrom(EntryInfoEntity t){
        if(t == null){
            return null;
        }
        EntryInfoReplicateCheckVO entryInfoReplicateCheckVO = new EntryInfoReplicateCheckVO();
        entryInfoReplicateCheckVO.setEntry(t.getEntry());
        entryInfoReplicateCheckVO.setProductID(t.getProductID());
        entryInfoReplicateCheckVO.setTag(t.getTag());
        entryInfoReplicateCheckVO.setComment(t.getComment());
        entryInfoReplicateCheckVO.setAbbr(t.getAbbr());
        entryInfoReplicateCheckVO.setImportType(t.getImportType());
        entryInfoReplicateCheckVO.setEntrySource(t.getEntrySource());
        entryInfoReplicateCheckVO.setDiFileName(t.getDiFileName());
        return entryInfoReplicateCheckVO;
    }

}
