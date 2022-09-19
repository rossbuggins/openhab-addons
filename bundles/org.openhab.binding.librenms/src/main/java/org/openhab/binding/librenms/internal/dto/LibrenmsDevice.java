package org.openhab.binding.librenms.internal.dto;

public class LibrenmsDevice {
    private int id;
    private Boolean online;

    public void setId(int id)
    {
        this.id = id;
    }
    
    public void setOnline(Boolean online)
    {
        this.online = online;
    }

    public int getId()
    {
        return this.id;
    }

    public Boolean getOnline()
    {
        return this.online;
    }
}
