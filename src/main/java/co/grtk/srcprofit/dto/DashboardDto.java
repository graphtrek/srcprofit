package co.grtk.srcprofit.dto;

public class DashboardDto {
    public InstrumentDto QQQ;
    public InstrumentDto GDX;
    public InstrumentDto IBIT;

    public InstrumentDto getQQQ() {
        return QQQ;
    }

    public void setQQQ(InstrumentDto QQQ) {
        this.QQQ = QQQ;
    }

    public InstrumentDto getGGX() {
        return GDX;
    }

    public void setGSX(InstrumentDto GDX) {
        this.GDX = GDX;
    }

    public InstrumentDto getIBIT() {
        return IBIT;
    }

    public void setIBIT(InstrumentDto IBIT) {
        this.IBIT = IBIT;
    }
}
