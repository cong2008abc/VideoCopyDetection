package Util;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Processor;

public class StateWaiter implements ControllerListener {
	Processor p;
	boolean error = false;

	public StateWaiter(Processor p) {
    	this.p = p;
    	p.addControllerListener(this);
	}

	public synchronized boolean waitForState(int state) {
    	switch (state) {
    	case Processor.Configured:
			p.configure(); break;
    	case Processor.Realized:
			p.realize(); break;
    	case Processor.Prefetched:
			p.prefetch(); break;
    	case Processor.Started:
			p.start(); break;
    	}

    	while (p.getState() < state && !error) {
			try {
	    		wait(1000);
			} catch (Exception e) {
			}
    	}
    	//p.removeControllerListener(this);
    	return !(error);
	}

	public void controllerUpdate(ControllerEvent ce) {
    	if (ce instanceof ControllerErrorEvent) error = true;
    	synchronized (this) {
			notifyAll();
    	}
	}
}
