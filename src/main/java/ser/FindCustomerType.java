package ser;

import com.ser.blueline.IDocument;
import com.ser.blueline.IInformationObject;
import com.ser.blueline.bpm.IProcessInstance;
import de.ser.doxis4.agentserver.UnifiedAgent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.util.List;
import java.util.stream.Collectors;

import static ser.Utils.getFirstDocInNode;
import static ser.Utils.loadGibCustomerTypes;


public class FindCustomerType extends UnifiedAgent {
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

            String cif = infoObj.getDescriptorValue(Conf.Descriptors.GIB_CIF, String.class);
            cif = (cif == null ? "" : cif);

            String accNr = infoObj.getDescriptorValue(Conf.Descriptors.GIB_AccountNumber, String.class);
            accNr = (accNr == null ? "" : accNr);

            String ctyp = "";
            String cval = infoObj.getDescriptorValue(Conf.Descriptors.GIB_CustomerType, String.class);
            cval = (cval == null ? "" : cval);

            IDocument disu = getFirstDocInNode(infoObj, "Issue");
            if(disu == null){throw new Exception("Issue-Doc not found.");}

            if(!cif.isEmpty() && !accNr.isEmpty()) {
                JSONObject cfgs = loadGibCustomerTypes();
                for (String ckey : cfgs.keySet()) {
                    JSONObject ccfg = cfgs.getJSONObject(ckey);
                    if(!cval.isEmpty()){
                        List<String> kyws = ccfg.getJSONArray("keywords").toList().stream().map(Object::toString).collect(Collectors.toList());
                        if(kyws.contains(cval)){
                            ctyp = ckey;
                            break;
                        }
                    }
                    if (!ccfg.has("classId")
                            || ccfg.getString("classId") == null
                            || ccfg.getString("classId").isEmpty()) {
                        continue;
                    }
                    String clsId = ccfg.getString("classId");
                    IInformationObject cust = getEFileCustomer(clsId, cif);
                    if(cust != null){
                        ctyp = ckey;
                        break;
                    }
                }
            }

            IProcessInstance proc = null;
            if(ctyp.isEmpty()){
                proc = helper.buildNewProcessInstanceForID(Conf.ProcessInstances.CustTypeDetermination);
                proc.setMainInformationObjectID(disu.getID());
                Utils.copyDescriptors(infoObj, proc);
                proc.setDescriptorValue(Conf.Descriptors.Project, disu.getID());
                proc.commit();
            }
            else {
                Utils.copyDescriptors(infoObj, disu);
                disu.setDescriptorValue(Conf.Descriptors.GIB_CustomerType, ctyp);
                disu.commit();

                infoObj.setDescriptorValue(Conf.Descriptors.GIB_CustomerType, ctyp);
                infoObj.setDescriptorValue(Conf.Descriptors.Status, "Ready");
                infoObj.commit();
            }

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
    private IInformationObject getEFileCustomer(String clsId, String cif) {
        StringBuilder builder = new StringBuilder();
        builder.append("TYPE = '").append(clsId).append("'")
                .append(" AND ")
                .append(Conf.DescriptorLiterals.CIF).append(" = '").append(cif).append("'");
        String whereClause = builder.toString();

        IInformationObject[] informationObjects = helper.createQuery(new String[]{Conf.Databases.Customer}, whereClause, "", 1, false);
        if(informationObjects.length < 1) {return null;}
        return informationObjects[0];
    }
}