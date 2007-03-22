/*
 * AbstractEventFilter.java
 *
 * Created on October 30, 2005, 4:58 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package ch.unizh.ini.caviar.eventprocessing;

import ch.unizh.ini.caviar.chip.*;
import java.beans.*;
import java.util.logging.Logger;
import java.util.prefs.*;

/**
 * An abstract class that all filters should subclass. Subclasses can be introspected to build a GUI to control the filter in {@link FilterPanel}.
 *<p>
 *The constructor that accepts a chip argument allows composing filters ala InputStream from java.io.
 *
 *@see FilterPanel
 * @author tobi
 */
public abstract class EventFilter {
    
    public EventProcessingPerformanceMeter  perf;
    
    protected Preferences prefs=Preferences.userNodeForPackage(EventFilter.class);
    protected PropertyChangeSupport support=new PropertyChangeSupport(this);
    protected Logger log=Logger.getLogger("EventFilter");
    
    /** true if filter is enclosed by another filter */
    private boolean enclosed=false;
    
    /** default false */
    protected boolean filterEnabled=false;
    
    /** true means the events are filtered in place, replacing the contents of the input packet and more
     *efficiently using memory. false means a new event packet is created and populated for the output of the filter.
     *<p>
     *default is false
     */
    protected boolean filterInPlaceEnabled=false;
    
    /** chip that we are filtering for */
    protected AEChip chip;
    
    
    public EventFilter(){
        perf=new EventProcessingPerformanceMeter(this);
//        setFilterEnabled(prefs.getBoolean(prefsKey(),false)); // this cannot easily be called here because it will be called during init of subclasses which have
        // not constructed themselves fully yet, e.g. field objects will not have been constructed. therefore, we set initial active states of all filters in FilterFrame after they are
        // all constructed with a Chip object.
    }
    
    /** Creates a new instance of AbstractEventFilter.
     *@param chip the chip to filter for
     */
    public EventFilter(AEChip chip){
        this.chip=chip;
        String key=this.getClass().getName()+".filterIsEnabled";
        setFilterEnabled(prefs.getBoolean(key, filterEnabled));
    }
    
    /** Returns the prefernces key for the filter
     @return "<SimpleClassName>.filterEnabled" e.g. DirectionSelectiveFilter.filterEnabled
     */
    public String prefsKey(){
        String key=this.getClass().getSimpleName()+".filterEnabled";
        return key;
    }
    
//    /**
//     * filters in to out. if filtering is enabled, the number of out may be less
//     * than the number put in
//     *@param in input events can be null or empty.
//     *@return the processed events, may be fewer in number. filtering may occur in place in the in packet.
//     */
//    abstract public AEPacket filter(AEPacket in) ;
    
//    abstract public ch.unizh.ini.caviar.aemonitor.AEPacket2D filter(ch.unizh.ini.caviar.aemonitor.AEPacket2D in);
    /** should return the filter state in some useful form */
    abstract public Object getFilterState() ;
    
    /** should reset the filter to initial state */
    abstract public void resetFilter() ;
    
    /** this should allocate and initialize memory: it may be called when the chip e.g. size parameters are changed after creation of the filter */
    abstract public void initFilter();
    
    /** @return true if filter is enabled */
    synchronized public boolean isFilterEnabled() {
        return filterEnabled;
    }
    
    /** @param enabled true to enable filter. false means output events are the same as input */
    synchronized public void setFilterEnabled(boolean enabled) {
        String key=prefsKey();
        prefs.putBoolean(key, enabled);
        support.firePropertyChange("filterEnabled",new Boolean(this.filterEnabled),new Boolean(enabled));
        this.filterEnabled=enabled;
        if(getEnclosedFilter()!=null){
            getEnclosedFilter().setFilterEnabled(filterEnabled);
        }
//        log.info(getClass().getName()+".setFilterEnabled("+filterEnabled+")");
    }
    
    /** @return the chip this filter is filtering for */
    public AEChip getChip() {
        return chip;
    }
    
    /** @param chip the chip to filter */
    public void setChip(AEChip chip) {
        this.chip = chip;
    }
    
    public PropertyChangeSupport getPropertyChangeSupport(){
        return support;
    }
    
    public boolean isFilterInPlaceEnabled() {
        return this.filterInPlaceEnabled;
    }
    
    public void setFilterInPlaceEnabled(final boolean filterInPlaceEnabled) {
        support.firePropertyChange("filterInPlaceEnabled",new Boolean(this.filterInPlaceEnabled),new Boolean(filterInPlaceEnabled));
        this.filterInPlaceEnabled = filterInPlaceEnabled;
    }
    
    protected EventFilter enclosedFilter;
    
    
    public EventFilter getEnclosedFilter() {
        return this.enclosedFilter;
    }
    
    /** Sets another filter to be enclosed inside this one - this enclosed filter should be applied first
     @param enclosedFilter the filter to enclose
     @see #setEnclosed
     */
    public void setEnclosedFilter(final EventFilter enclosedFilter) {
        this.enclosedFilter = enclosedFilter;
        if(enclosedFilter!=null) enclosedFilter.setEnclosed(true);
    }
    
    protected boolean annotationEnabled=true;
    
    public boolean isAnnotationEnabled() {
        return annotationEnabled;
    }
    
    /**@param annotationEnabled true to draw annotations */
    public void setAnnotationEnabled(boolean annotationEnabled) {
        this.annotationEnabled = annotationEnabled;
    }
    
    /** @return true if this filter is enclosed inside another */
    public boolean isEnclosed() {
        return enclosed;
    }
    
    /** Sets marker to show this instance is enclosed
     @param enclosed true if this filter is enclosed
     */
    public void setEnclosed(boolean enclosed) {
        this.enclosed = enclosed;
    }
    
}
