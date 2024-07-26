package junit

import de.ser.doxis4.agentserver.AgentExecutionResult
import org.junit.*
import ser.FindCustomerType

class TEST_FindCustomerType {

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
        def agent = new FindCustomerType()

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SR08GIB_JIRA2433f09aaf-1fb9-4886-9ef9-1af9f8954206182024-07-25T17:27:08.029Z011"

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
