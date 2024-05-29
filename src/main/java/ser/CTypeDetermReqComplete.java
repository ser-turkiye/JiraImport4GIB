package ser;

import com.ser.blueline.*;
import com.ser.blueline.bpm.IProcessInstance;
import com.ser.blueline.bpm.ITask;
import com.ser.blueline.metaDataComponents.IArchiveClass;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;


public class CTypeDetermReqComplete extends UnifiedAgent {
    Logger log = LogManager.getLogger();
    IProcessInstance processInstance;
    IInformationObject qaInfObj;
    ProcessHelper helper;
    ITask task;
    IInformationObject document;
    String compCode;
    String reqId;
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
        Utils.loadDirectory(Conf.Paths.MainPath);
        
        task = getEventTask();

        try {

            helper = new ProcessHelper(Utils.session);
            XTRObjects.setSession(Utils.session);

            processInstance = task.getProcessInstance();
            document = processInstance.getMainInformationObject();
            if(document == null){
                List<ILink> aLnks = processInstance.getLoadedInformationObjectLinks().getLinks();
                document = (!aLnks.isEmpty() ? aLnks.get(0).getTargetInformationObject() : document);
                if(document != null) {
                    processInstance.setMainInformationObjectID(document.getID());
                }
            }
            if(document == null){throw new Exception("Issue-Document not found.");}

            String ctyp = processInstance.getDescriptorValue(Conf.Descriptors.GIB_CustomerType, String.class);
            ctyp = (ctyp == null ? "" : ctyp);
            if(ctyp.isEmpty()){throw new Exception("_CustomerType not set.");}

            String cprj = document.getDescriptorValue(Conf.Descriptors.Project, String.class);
            cprj = (cprj == null ? "" : cprj);
            if(cprj.isEmpty()){throw new Exception("ObjectName not set.");}

            String diky = document.getDescriptorValue(Conf.Descriptors.ParentID, String.class);
            diky = (diky == null ? "" : diky);
            if(diky.isEmpty()){throw new Exception("ObjectDocumentReference not set.");}

            IInformationObject issue = getEFileIssue(cprj, diky);
            if(issue == null){throw new Exception("Issue EFile not found.");}

            issue.setDescriptorValue(Conf.Descriptors.GIB_CustomerType, ctyp);
            issue.setDescriptorValue(Conf.Descriptors.Status, "Ready");
            issue.commit();

            document.setDescriptorValue(Conf.Descriptors.GIB_CustomerType, ctyp);
            document.commit();

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

    private IInformationObject getEFileIssue(String cprj, String docId) {
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(Conf.ClassIDs.EFile).append("'")
                .append(" AND ")
                .append(Conf.DescriptorLiterals.Project).append(" = '").append(cprj).append("'")
                .append(" AND ")
                .append(Conf.DescriptorLiterals.DocID).append(" = '").append(docId).append("'");
        String whereClause = builder.toString();

        IInformationObject[] informationObjects = helper.createQuery(new String[]{Conf.Databases.Jira}, whereClause, "", 1, false);
        if(informationObjects.length < 1) {return null;}
        return informationObjects[0];
    }
    public static IDocument copyDocument(IInformationObject docu, String dtyp) throws Exception {
        IArchiveClass ac = Utils.server.getArchiveClassByName(Utils.session, dtyp.trim());
        if(ac == null){throw new Exception("ArchiveClass not found (Name : " + dtyp + ")");}
        IDatabase db = Utils.session.getDatabase(ac.getDefaultDatabaseID());
        IDocument rtrn = Utils.server.getClassFactory().getDocumentInstance(db.getDatabaseName(), ac.getID(), "0000" , Utils.session);
        rtrn = Utils.server.copyDocument2(Utils.session, (IDocument) docu, rtrn, CopyScope.COPY_PART_DOCUMENTS);
        return rtrn;
    }
}