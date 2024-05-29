package junit

import de.ser.doxis4.agentserver.AgentExecutionResult
import org.junit.*
import ser.DocClassification

class TEST_DocClassification {

    Binding binding

    @BeforeClass
    static void initSessionPool() {
        AgentTester.initSessionPool()
    }

    @Before
    void retrieveBinding() {
        binding = AgentTester.retrieveBinding()
    }

    @Test
    void testForAgentResult() {
        def agent = new DocClassification()

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SR08GIB_JIRA24a2b7bb77-61e2-4b0c-bead-f2dddf6d4809182024-05-28T04:53:50.590Z011"

        def result = (AgentExecutionResult)agent.execute(binding.variables)
        assert result.resultCode == 0
    }

    @Test
    void testForJavaAgentMethod() {
        //def agent = new JavaAgent()
        //agent.initializeGroovyBlueline(binding.variables)
        //assert agent.getServerVersion().contains("Linux")
    }

    @After
    void releaseBinding() {
        AgentTester.releaseBinding(binding)
    }

    @AfterClass
    static void closeSessionPool() {
        AgentTester.closeSessionPool()
    }
}
