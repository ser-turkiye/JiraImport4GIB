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

        binding["AGENT_EVENT_OBJECT_CLIENT_ID"] = "SD08GIB_DOCS24f5483a11-0b89-41a0-8483-570590d52328182024-05-17T05:17:39.861Z011"

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
