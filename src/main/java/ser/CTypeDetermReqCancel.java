package ser;

import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;


public class CTypeDetermReqCancel extends UnifiedAgent {
    Logger log = LogManager.getLogger();
    IProcessInstance processInstance;
    ProcessHelper helper;
    ITask task;
    @Override
    protected Object execute() {
        if (getEventTask() == null)
            return resultError("Null Document object");

        if(getEventTask().getProcessInstance().findLockInfo().getOwnerID() != null){
            return resultRestart("Restarting Agent");
        }

        Utils.session = getSes();
        Utils.bpm = getBpm();
        Utils.server = Utils.session.getDocumentServer();
        
        task = getEventTask();

        try {

            helper = new ProcessHelper(Utils.session);
            XTRObjects.setSession(Utils.session);

            processInstance = task.getProcessInstance();

            /*
            document = processInstance.getMainInformationObject();
            if(document == null){
                List<ILink> aLnks = processInstance.getLoadedInformationObjectLinks().getLinks();
                document = (!aLnks.isEmpty() ? aLnks.get(0).getTargetInformationObject() : document);
                if(document != null) {
                    processInstance.setMainInformationObjectID(document.getID());
                }
            }
            if(document == null){
                throw new Exception("Document not found.");
            }
            */

            //XTRObjects.deleteDocument(document);

            log.info("Tested.");

        } catch (Exception e) {
            //throw new RuntimeException(e);
            log.error("Exception       : " + e.getMessage());
            log.error("    Class       : " + e.getClass());
            log.error("    Stack-Trace : " + Arrays.toString(e.getStackTrace()));
            return resultError("Exception : " + e.getMessage());
        }

        log.info("Finished");
        return resultSuccess("Ended successfully");
    }
}