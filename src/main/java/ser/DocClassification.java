package ser;

import com.ser.blueline.*;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.IWorkbasket;
import com.ser.blueline.metaDataComponents.IArchiveClass;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.util.List;

import static ser.Utils.*;


public class DocClassification extends UnifiedAgent {
    Logger log = LogManager.getLogger();
    IInformationObject qaInfObj;
    ProcessHelper helper;
    IInformationObject infoObj;
    @Override
    protected Object execute() {
        if (getEventInfObj() == null)
            return resultError("Null Document object");

        Utils.session = getSes();
        Utils.bpm = getBpm();
        Utils.server = Utils.session.getDocumentServer();
        Utils.loadDirectory(Conf.Paths.MainPath);

        infoObj = getEventInfObj();

        try {
            helper = new ProcessHelper(Utils.session);
            XTRObjects.setSession(Utils.session);
            XTRObjects.setBpm(Utils.bpm);

            String ctyp = infoObj.getDescriptorValue(Conf.Descriptors.GIB_CustomerType, String.class);
            ctyp = (ctyp == null ? "" : ctyp);
            if(ctyp.isEmpty()){throw new Exception("Customer-Type not found.");}

            JSONObject wcfs = Utils.loadGIBJiraTicketClassifications(ctyp);

            List<IInformationObject> list = Utils.getAllDocInNode(infoObj, "Attachments");
            for(IInformationObject atch : list){
                run4Document(infoObj, ctyp, atch, wcfs);
            }

            infoObj.setDescriptorValue(Conf.Descriptors.Status, "Done");
            infoObj.commit();

            log.info("Tested.");

        } catch (Exception e) {
            //throw new RuntimeException(e);
            log.error("Exception       : " + e.getMessage());
            log.error("    Class       : " + e.getClass());
            log.error("    Stack-Trace : " + e.getStackTrace() );
            return resultError("Exception : " + e.getMessage());
        }

        log.info("Finished");
        return resultSuccess("Ended successfully");
    }
    private void run4Document(IInformationObject infoObj, String ctyp, IInformationObject document, JSONObject wcfs) throws Exception {

        String fnam = document.getDescriptorValue(Conf.Descriptors.Name, String.class);
        JSONObject mcfg = null;
        for(String wkey : wcfs.keySet()){
            JSONObject wcfg = wcfs.getJSONObject(wkey);
            if(!wcfg.has("priority")){continue;}
            if(wcfg.getInt("priority") <= 0){continue;}

            String wcrd = wkey.toUpperCase();
            wcrd = wcrd.replaceAll("\\.", "");
            wcrd = wcrd.replaceAll("\\*", ".*");
            if(!fnam.toUpperCase().matches(wcrd)){continue;}
            if(mcfg != null && mcfg.getInt("priority") <= wcfg.getInt("priority")){continue;}
            mcfg = wcfg;
        }

        if(mcfg == null){
            IProcessInstance proc = helper.buildNewProcessInstanceForID(Conf.ProcessInstances.DocumentDefinition);
            proc.setMainInformationObjectID(document.getID());
            Utils.copyDescriptors(document, proc);
            Utils.copyDescriptors(infoObj, proc);
            proc.setDescriptorValue("ObjectName", document.getID());
            proc.commit();
            return;
        }

        String mfld = (mcfg.has("mainFolder") && mcfg.getString("mainFolder") != null ?
                mcfg.getString("mainFolder") : "");
        if (mfld.isEmpty()) {
            throw new Exception("Wildcard.mainFolder is empty.");
        }

        String dtyp = (mcfg.has("docType") && mcfg.getString("docType") != null ?
                mcfg.getString("docType") : "");

        document.setDescriptorValue(Conf.Descriptors.GIB_MainFolder, mfld);
        document.setDescriptorValue(Conf.Descriptors.GIB_DocumentType, dtyp);
        document.setDescriptorValue(Conf.Descriptors.GIB_CustomerType, ctyp);
        document.commit();

        IDocument docGIB = copyDocument(document, mfld);
        Utils.copyDescriptors(document, docGIB);
        Utils.copyDescriptors(infoObj, docGIB);
        docGIB.commit();
    }
    public static IDocument copyDocument(IInformationObject docu, String dtyp) throws Exception {
        IArchiveClass ac = Utils.server.getArchiveClassByName(Utils.session, dtyp);
        if(ac == null){throw new Exception("ArchiveClass not found (Name : " + dtyp + ")");}
        IDatabase db = Utils.session.getDatabase(ac.getDefaultDatabaseID());
        IDocument rtrn = Utils.server.getClassFactory().getDocumentInstance(db.getDatabaseName(), ac.getID(), "0000" , Utils.session);
        rtrn = Utils.server.copyDocument2(Utils.session, (IDocument) docu, rtrn, CopyScope.COPY_PART_DOCUMENTS);
        return rtrn;
    }
}