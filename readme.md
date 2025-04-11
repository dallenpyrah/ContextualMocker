Design Document: ContextualMocker - A Parallel-Safe, Context-Aware Java Mocking Framework1. IntroductionModern software applications, particularly those built on microservices or handling concurrent user requests, frequently rely on shared components, often implemented as singletons (e.g., service beans in dependency injection frameworks). Testing these applications effectively, especially under concurrent load, presents significant challenges for existing Java mocking frameworks.Current tools, while powerful for traditional unit testing, exhibit limitations when dealing with concurrent operations on shared mock instances. Issues such as race conditions during stubbing or verification lead to flaky, unreliable tests, undermining developer confidence.1 Furthermore, these frameworks lack native support for context-aware mocking – the ability to define mock behavior based on the specific operational context (e.g., user session, request ID, tenant ID) of a concurrent request interacting with a shared component. Developers resort to complex workarounds, often involving intricate Answer implementations or fragile parameter matching, which obscure test intent and are difficult to maintain.2This document outlines the design for ContextualMocker, a new, hypothetical open-source Java mocking framework engineered specifically to address these shortcomings. ContextualMocker aims to provide robust, parallel-safe mocking capabilities for shared instances, coupled with a first-class, intuitive API for defining context-aware behavior and verification. It targets developers building and testing concurrent Java applications who require reliable and expressive mocking tools for shared dependencies operating under varying contexts.2. The Core Problem: Concurrency and Context Limitations in Existing FrameworksThe fundamental motivation for ContextualMocker stems from two interconnected deficiencies in the current Java mocking landscape: the lack of reliable thread safety for concurrent operations on shared mocks, and the absence of native mechanisms for context-aware mocking.

Concurrency Challenges with Shared Mocks:

Mockito's Thread-Safety Issues: While Mockito is the de facto standard Java mocking framework 3, its design exhibits well-documented thread-safety problems when stubbing or verifying a shared mock instance concurrently from multiple threads.1 Although concurrent invocations on an already-stubbed mock are generally considered safe 5, the setup (stubbing) and assertion (verification) phases are problematic. Numerous issue reports and discussions highlight intermittent failures like UnfinishedStubbingException, WrongTypeOfReturnValue, or incorrect verification counts when tests involving shared mocks are run in parallel.7
Underlying Cause: These issues appear rooted in Mockito's internal reliance on thread-local state to manage the process of stubbing and verification (e.g., tracking the "last invocation" before thenReturn or verify).8 When multiple threads attempt to stub or verify the same shared mock instance simultaneously, this thread-local mechanism leads to race conditions. One thread's operation can interfere with another's, corrupting Mockito's internal state.8
Workarounds and Their Limits: Developers have attempted workarounds, such as external synchronization using locks around Mockito calls 7, avoiding shared mocks altogether by creating mocks per test or per thread (often using ThreadLocal) 11, or using Spring Boot's @MockBean(reset = MockReset.NONE).13 However, these solutions add significant complexity, boilerplate, or only address specific scenarios, failing to provide a general, framework-level guarantee of safety. Other frameworks like EasyMock and JMockit also face challenges or lack explicit support for robust parallel testing on shared mocks.15 Spock, while supporting parallel execution via the JUnit Platform 19, requires careful use of @ResourceLock for shared state and may have issues with shared spies.20



The Need for Explicit Context-Awareness:

Modern Application Complexity: Concurrent applications often process multiple independent operations simultaneously (e.g., handling web requests for different users, processing messages for various tenants). Shared components (singletons) involved in these operations frequently need to behave differently based on the context of the specific operation (e.g., return user-specific data based on userId, apply tenant-specific rules based on tenantId, correlate logs via requestId).21
Limitations of Implicit Context: Existing mocking frameworks lack first-class support for defining mock behavior based on such application-level contexts. Developers are forced to simulate context-awareness indirectly. Common approaches include:

Complex Answer Implementations: Writing custom Answer logic that inspects method arguments to infer context and return different values.2 This couples tests tightly to implementation details (argument order/type) and makes tests verbose and hard to read.
Argument Matching: Using intricate argument matchers to differentiate calls based on context data passed as parameters. This also leads to brittle and less readable tests.
Stateful Mocks: Employing stateful mocking techniques where the mock transitions between states based on interactions, indirectly representing context changes.23 This often requires significant setup and doesn't map cleanly to distinct, independent contexts.


Contextual Mocking Decisions: Research indicates that mocking decisions themselves are often context-dependent.32 A dependency might be mocked in one test scenario but not another, based on how it interacts with the class under test (CUT) in that specific context. Inappropriate mocking (under-mocking or over-mocking) can lead to ineffective tests or maintenance burdens.32 ContextualMocker aims to provide a mechanism to manage these context-specific behaviors explicitly.
Existing Contextual Tools: The need for context management in testing is recognized in specific domains. For example, AEM Mocks provide an AemContext object to manage the AEM-specific environment for tests 33, and Ctest4J links configuration parameters (a form of context) to specific tests.34 These demonstrate the value of context management, highlighting the gap for a general-purpose mocking solution.


The challenges of concurrency safety and context awareness are not independent; they converge critically in the testing of modern applications. Concurrent requests or operations are often the distinct contexts that necessitate different behaviors from shared components. Testing shared singletons under realistic parallel load inherently requires the ability to define mock behavior specific to the context of each concurrent operation. Existing frameworks fail on two fronts: concurrent stubbing/verification on the shared mock instance breaks due to internal thread-safety issues, and even if these operations were safe, defining and managing behavior per-context remains cumbersome and lacks dedicated API support.Furthermore, it is important to distinguish the goals of a mocking framework like ContextualMocker from specialized concurrency testing tools. Tools like RaceFuzzer 35, Fray 36, MAPTest 37, or jcstress 38 focus on exploring thread interleavings, detecting data races or deadlocks, and controlling thread scheduling, often by instrumenting code or manipulating the runtime environment without necessarily mocking dependencies.36 ContextualMocker, conversely, focuses on providing controllable behavior (via mocking) for dependencies within a potentially concurrent execution environment. Its primary contribution lies in robust internal state management that allows context-specific behavior definition and verification, addressing the specific shortcomings of mocking frameworks in concurrent settings. It assumes the underlying test execution (potentially parallel) is managed by the test runner (e.g., JUnit, TestNG) or potentially integrated with concurrency control tools in the future, rather than controlling thread scheduling itself.The following table summarizes the capabilities and limitations of prominent existing frameworks concerning parallel execution safety and context awareness, highlighting the gap ContextualMocker aims to fill.Table 2.1: Feature Comparison: Concurrency & Context in Mocking Frameworks
FeatureMockitoEasyMockJMockitSpockWireMock (Server)ContextualMocker (Proposed)Parallel Test Runner SupportYes (JUnit/TestNG) 4Limited/Issues 15No/Problematic 16Yes (JUnit Platform) 19Yes (Server) 43YesShared Mock Stubbing SafetyNo 1Limited/Issues 45No/Problematic 16Issues w/ shared/spy 20Yes (via API/Scenarios) 47YesShared Mock Verification SafetyNo 1Limited/Issues 45No/Problematic 16Yes (w/ @ResourceLock) 19Yes (via API/Scenarios) 47YesExplicit Context APINoNoNoNoLimited (Scenarios/Proxy) 47YesStateful Mocking (Basic)Yes (via Answer) 23YesYesYesYes (Scenarios) 47YesContextual Stateful MockingNoNoNoNoLimited (via Scenarios) 47Yes
3. ContextualMocker: Design Philosophy and GoalsContextualMocker is guided by a set of core principles aimed at providing a robust, intuitive, and performant solution for mocking in concurrent, context-driven environments.

Safety First for Concurrency: The paramount goal is to guarantee thread safety and deterministic behavior for all framework operations, particularly concurrent stubbing and verification targeting shared mock instances. The design must proactively eliminate the race conditions and flakiness observed in existing tools.1 Reliability in concurrent scenarios is non-negotiable and foundational to the framework's value proposition. This necessitates a shift away from designs relying implicitly on thread-local state for core operations, towards explicit, thread-safe state management.


Intuitive Context Management: Context should be treated as a first-class citizen within the framework. The API must provide clear, explicit, and user-friendly mechanisms for defining the context associated with a mock interaction (stubbing or verification). The goal is to make specifying context-dependent behavior straightforward and highly readable, reflecting the reality that mocking decisions are often context-aware.32 Complex boilerplate or overly implicit context handling should be avoided in favor of clarity and safety.


Expressive and Fluent API: The public APIs for stubbing and verification should be designed for fluency and readability, promoting maintainable test code. Where applicable, the API should align with established patterns like Behavior-Driven Development (Given-When-Then).50 Lessons learned from the success and usability of Mockito's API design 3 should be incorporated, adapted for the requirements of context management and concurrency safety.


Performance and Scalability: While correctness and safety are primary, the framework must be designed with performance and scalability in mind, especially for highly concurrent test scenarios. The internal architecture should minimize lock contention and reduce memory overhead.52 Efficient concurrency primitives, such as ConcurrentHashMap and its atomic operations 53, should be leveraged. Performance benchmarks under load will be essential to validate the design.55


Extensibility and Integration: The framework should offer well-defined extension points (Service Provider Interfaces - SPIs) to facilitate integration with various test runners (e.g., JUnit 5 57, TestNG 11), context propagation frameworks (e.g., those using MDC 21 or other mechanisms), and potentially other testing or diagnostic tools. This allows for customization and broader adoption, learning from Mockito's extension mechanisms like MockMaker 61 and MockitoListener.63

A key consideration influencing these principles is the inherent trade-off between implicit convenience and explicit safety. Mockito's original API, exemplified by when(mock.method()).thenReturn(...), prioritized convenience by implicitly managing stubbing state via thread-locals. This worked well in single-threaded tests but proved fragile under concurrency.1 In contrast, approaches like WireMock's rely on explicit state management within the mock server.47 ContextualMocker must deliberately favor explicitness in its core state management for concurrency and context handling. The safety guarantees provided by explicit context identification and thread-safe central storage outweigh the minor increase in API verbosity compared to the simplest single-threaded Mockito usage. Fluency will be achieved through the overall API structure (e.g., given(...).forContext(...).when(...).thenReturn(...)) rather than by hiding the underlying state management complexities.4. Core ArchitectureThe architecture of ContextualMocker is designed around a central, thread-safe registry that manages mock state based on both the mock instance and the specific context of interaction.

4.1 Mock Instantiation:

Mechanism: Mock instances will be created using bytecode manipulation, generating proxies that intercept method calls. ByteBuddy is the preferred library for this task, given its capabilities, active maintenance, and successful adoption by Mockito 2+ and Spock.65
Process: When ContextualMocker.mock() is called, ByteBuddy generates a proxy class extending or implementing the target type. This proxy holds a reference to a central invocation handler.
Extensibility: A ContextualMockMaker interface, analogous to Mockito's MockMaker 61, could be provided as an extension point for custom mock creation strategies, ensuring the design accommodates context requirements.



4.2 Invocation Interception:

Mechanism: All method calls on a mock instance are intercepted by the generated ByteBuddy proxy.
Handler Delegation: The proxy delegates every intercepted invocation to a shared (but internally thread-safe) ContextualInvocationHandler instance associated with the mock. This handler is the central point for processing incoming calls.



4.3 Thread-Safe, Context-Aware State Management (CRUCIAL):

Central Component: A singleton MockRegistry instance serves as the global repository for all mock state managed by the framework. This registry must be implemented using thread-safe constructs.
Data Structures: The core of the registry involves nested concurrent maps to store stubbing rules and invocation records, keyed first by the mock instance and then by the context ID.

Stubbing Rules:
Java// Key: Weak ref to mock instance, Value: Map of ContextID -> Rules
ConcurrentMap<WeakReference<Object>, ConcurrentMap<Object, List<StubbingRule>>> stubbingRules;


WeakReference<Object>: Using a weak reference to the mock instance allows the mock object to be garbage collected if it's no longer referenced elsewhere, preventing memory leaks.
Object (Inner Map Key): Represents the ContextID. This can be any user-defined object (String, Long, custom type) provided it correctly implements equals() and hashCode().
List<StubbingRule>: A thread-safe list containing the ordered stubbing rules for that specific mock/context pair. Each StubbingRule encapsulates method matchers, argument matchers, and the corresponding Answer or return value. The choice of list implementation (e.g., CopyOnWriteArrayList, synchronized ArrayList, ConcurrentLinkedQueue) depends on the expected read/write ratio for stubbing operations. CopyOnWriteArrayList is efficient if stubbing is infrequent compared to invocation handling reads.


Invocation Records:
Java// Key: Weak ref to mock instance, Value: Map of ContextID -> Invocations
ConcurrentMap<WeakReference<Object>, ConcurrentMap<Object, BlockingQueue<InvocationRecord>>> invocationRecords;


BlockingQueue<InvocationRecord>: A thread-safe queue (e.g., ConcurrentLinkedQueue or LinkedBlockingQueue) is suitable for recording invocations, as this is primarily a high-frequency write operation during test execution. Using a queue facilitates ordered recording. Sharding or other strategies might be needed for extreme throughput scenarios.
InvocationRecord: An immutable object capturing invocation details (mock reference, method, arguments, context ID, timestamp, thread ID).




Concurrency Control:

Leverage ConcurrentHashMap's inherent thread safety and atomic operations (computeIfAbsent, compute, merge) for managing the nested map structures.53 This provides efficient, fine-grained concurrency control for accessing state related to different mocks or contexts.
Operations modifying the List<StubbingRule> for a specific mock/context must be atomic. If CopyOnWriteArrayList isn't suitable due to frequent stubbing changes, explicit locking (e.g., a ReentrantLock associated with the inner map entry) might be needed around list modifications.
Adding to the invocationRecords queue should be highly concurrent; ConcurrentLinkedQueue is a strong candidate.
Avoid global locks on the entire MockRegistry. Locking should be scoped as narrowly as possible, ideally at the level of a specific mock instance or mock/context pair. Contrast this with the performance limitations of single-lock structures like Hashtable or Collections.synchronizedMap.56


Rationale: This architecture directly maps the core requirement: state isolation per mock and per context, managed concurrently. ConcurrentHashMap provides a robust and performant foundation.53 Weak references prevent memory leaks often associated with global registries holding onto mock objects.



4.4 Context Identification Strategies: Determining the correct ContextID for an invocation is critical. Several strategies are possible, each with trade-offs:

Strategy 1: Explicit Context Passing (Recommended Default):

Mechanism: The test author explicitly provides the ContextID object through the API, such as ContextualMocker.given(mock).forContext(myContextId)....
Pros: Most robust and unambiguous. Guarantees thread safety regardless of execution model (thread pools, async frameworks). Makes context dependency explicit in the test code.
Cons: Requires the test author to manage and pass the context identifier, potentially increasing verbosity.
Implementation: The passed ContextID is used directly as the key for the inner ConcurrentHashMap in the MockRegistry.


Strategy 2: Implicit Context Capture via ThreadLocal:

Mechanism: The framework offers utilities like ContextualMocker.runInContext(contextId, () -> { /* test code */ }) or integrates with JUnit/TestNG extensions 33 to manage a ThreadLocal<ContextID>. Framework methods like when(...) or verify(...) would implicitly retrieve the context from this ThreadLocal.
Pros: Can lead to less verbose API calls within the contextual block. Feels more "automatic".
Cons: Inherently fragile in asynchronous execution environments or applications using thread pools where threads are reused, as the ThreadLocal value might leak or be incorrect.21 Requires extremely careful setup and cleanup (e.g., using ThreadLocal.remove() in finally blocks or via test framework extensions 33). Can obscure the dependency on context. Potential for memory leaks if remove() is not diligently called.22 Mockito's own configuration uses ThreadLocal per-thread storage, hinting at potential complexities.74
Implementation: The ContextualInvocationHandler reads from the ThreadLocal. Requires robust lifecycle management, ideally automated via extensions.


Strategy 3: Framework Integration Hooks:

Mechanism: Define SPIs (e.g., ContextResolver) or listener interfaces (akin to MockitoListener 63 or AEM Context Plugins 33) that allow external frameworks (e.g., web frameworks, context propagation libraries) to provide the ContextID.
Pros: Enables seamless integration with existing application context mechanisms (e.g., retrieving a request ID from MDC 21).
Cons: Requires specific integration code to be developed for each supported framework. Can be complex to implement correctly.
Implementation: The registered hook/listener is invoked by the ContextualInvocationHandler to obtain the current ContextID.


Default Choice & Rationale: Explicit Context Passing is recommended as the default strategy due to its superior robustness, predictability, and safety across diverse execution environments. ThreadLocal support can be offered as an optional, advanced feature, but must be accompanied by prominent warnings regarding its limitations and the critical need for proper lifecycle management. Framework hooks provide a path for deeper integration but are secondary to the core explicit mechanism.


The choice of internal data structures directly impacts scalability. While ConcurrentHashMap offers good baseline performance 53, high-contention scenarios, particularly involving frequent invocation recording on the same shared mock/context, might necessitate further optimization. Profiling under realistic load is essential.55 Techniques like sharding the invocation record lists (e.g., using multiple queues based on ContextID hash) or exploring lock-free queue implementations could be considered if the default ConcurrentHashMap/ConcurrentLinkedQueue approach proves insufficient. Benchmarking against alternatives will be key.76Furthermore, the framework must be flexible regarding the type used for ContextID. It could range from simple types like String or Long to complex, user-defined objects. The critical requirement is that any object used as a ContextID must implement equals() and hashCode() correctly and consistently, as it serves as a key in the underlying ConcurrentHashMap. Using mutable objects as context IDs is strongly discouraged due to the potential for unpredictable behavior if their state changes after being used as a key. The framework should document this requirement clearly and perhaps provide standard implementations for common cases (e.g., StringContextId, ThreadIdContextId).The following table compares the context identification strategies:Table 4.1: Context Identification Strategy ComparisonFeatureExplicit API PassingImplicit ThreadLocalFramework HooksThread SafetyHigh (Context passed directly)Low (Requires careful management)Variable (Depends on hook impl.)Ease of Use (Verbosity)Medium (Requires forContext() call)Low (Implicit within context block)Low (If integration exists)Environment CompatibilityHigh (Works everywhere)Low (Fails easily in thread pools/async)Variable (Depends on integration)Context PropagationUser ResponsibilityFramework/User ResponsibilityIntegration ResponsibilityPotential for ErrorsLowHigh (Leaks, incorrect context)Medium (Integration complexity)5. Public API SpecificationThe public API aims for fluency, readability, and explicit context handling, drawing inspiration from Mockito where appropriate but adapting for concurrency and context.

5.1 Entry Point (ContextualMocker):

A static utility class, similar to org.mockito.Mockito, serving as the main entry point.
Mock Creation: public static <T> T mock(Class<T> classToMock) - Creates a standard mock instance managed by the framework. Overloads for mock settings (name, default answer per context?) could be considered.
Stubbing: public static <T> ContextualStubbingInitiator<T> given(T mock) - Initiates the stubbing process for a given mock.
Verification: public static <T> ContextualVerificationInitiator<T> verify(T mock) - Initiates the verification process for a given mock.



5.2 Contextual Stubbing API: Designed as a fluent chain starting from given().

ContextualStubbingInitiator<T> given(T mock): Takes the mock, returns initiator.
ContextSpecificStubbingInitiator<T> forContext(ContextID contextId): Crucial step. Specifies the context. Returns context-specific initiator.

Optional Alternative: ContextSpecificStubbingInitiator<T> forCurrentContext(): Uses ThreadLocal context if enabled.


OngoingContextualStubbing<R> when(Function<T, R> methodCall): Defines the method call via lambda (e.g., when(m -> m.getUser("id1"))). Returns ongoing stubbing object.
Terminal Methods (complete the stubbing):

void thenReturn(R value): Specifies a return value.
void thenThrow(Throwable throwable): Specifies an exception.
void thenAnswer(ContextualAnswer<R> answer): Specifies a custom Answer. The ContextualAnswer interface could extend Mockito's Answer 2, potentially receiving the ContextID and InvocationOnMock in its answer method.


Example:
JavaContextID user1Context = new StringContextId("user1");
ContextID user2Context = new StringContextId("user2");
UserService mockService = ContextualMocker.mock(UserService.class);

// Stub for user1
ContextualMocker.given(mockService)
   .forContext(user1Context)
   .when(service -> service.getUserData("dataKey"))
   .thenReturn("User1 Data");

// Stub for user2
ContextualMocker.given(mockService)
   .forContext(user2Context)
   .when(service -> service.getUserData("dataKey"))
   .thenReturn("User2 Data");

// Stub with Answer for user1
ContextualMocker.given(mockService)
   .forContext(user1Context)
   .when(service -> service.processData(anyString()))
   .thenAnswer((invocation, context) -> { // ContextualAnswer example
        String input = invocation.getArgument(0);
        // Logic potentially using context (user1Context)
        return "Processed for User1: " + input;
    });


Thread Safety: The entire fluent chain, culminating in thenReturn/thenThrow/thenAnswer, must result in an atomic update to the stubbingRules map in the MockRegistry for the specified mock and context ID.



5.3 Contextual Verification API: Designed as a fluent chain starting from verify().

ContextualVerificationInitiator<T> verify(T mock): Takes the mock, returns initiator.
ContextSpecificVerificationInitiator<T> forContext(ContextID contextId): Crucial step. Specifies the context. Returns context-specific initiator.

Optional Alternative: ContextSpecificVerificationInitiator<T> forCurrentContext(): Uses ThreadLocal context if enabled.


Verification Modes (operate within the specified context): Methods like times(int n), never(), atLeastOnce(), atLeast(int n), atMost(int n), only(). These return a ContextualVerificationMode<T> object. (Adapts standard Mockito modes 79).
T method(Consumer<T> methodCall): Called on the ContextSpecificVerificationInitiator or ContextualVerificationMode to specify the method invocation to verify (e.g., .method(service -> service.getUserData("dataKey"))). This performs the actual verification against the recorded invocations for the context.
No Interaction Verification (within context):

void verifyNoMoreInteractions(T mock, ContextID contextId): Asserts no unverified interactions remain for the mock in this context.
void verifyNoInteractions(T mock, ContextID contextId): Asserts no interactions occurred at all for the mock in this context.


Example:
Java// Verify user1 interaction
ContextualMocker.verify(mockService)
   .forContext(user1Context)
   .times(1)
   .method(service -> service.getUserData("dataKey"));

// Verify user2 interaction never happened
ContextualMocker.verify(mockService)
   .forContext(user2Context)
   .never()
   .method(service -> service.processData("some data"));

// Verify no other interactions for user1
ContextualMocker.verifyNoMoreInteractions(mockService, user1Context);


Thread Safety: Verification involves reading from the invocationRecords. This read operation must be thread-safe. The verification logic itself should be careful about concurrent modifications to the invocation list if the test is verifying while the SUT might still be running (though typically verification happens after the action).



5.4 Context Management API: Primarily relevant if the implicit ThreadLocal strategy is enabled and used.

Explicit Lifecycle:

ContextualMocker.registerContext(ContextID contextId): Binds context to the current thread.
ContextualMocker.unregisterContext(): Clears context from the current thread (calls ThreadLocal.remove() 22). Essential for preventing leaks in pooled/reused threads.
ContextualMocker.runInContext(ContextID contextId, Runnable task) / callInContext(ContextID contextId, Callable<V> task): Convenience methods ensuring proper registration and cleanup around a block of code.


Integration: Define SPIs (e.g., ContextResolverProvider) or listener interfaces (e.g., TestLifecycleListener) for integration with test runners or external context frameworks.



5.5 Argument Matcher Handling:

Compatibility: Leverage standard Mockito ArgumentMatchers (e.g., any(), eq(), anyString(), argThat()) for familiarity.82
Thread Safety: Mockito's matcher definition process often uses a thread-local stack.83 The ContextualMocker API implementation (specifically the when() and method() steps in stubbing/verification) must ensure that concurrent API calls from different threads do not interfere with each other's matcher registration process. This might involve synchronization around the parts of the API call that interact with Mockito's internal matcher state or ensuring that the state is properly isolated per API call chain. The ContextualInvocationHandler must also correctly capture and apply the matchers associated with a specific StubbingRule.


The design of the fluent API introduces intermediate objects (e.g., ContextualStubbingInitiator). If these objects hold state (like the contextId), they must be designed carefully. Ideally, a complete fluent call chain (given...thenReturn) should be executed atomically within a single thread. Sharing these intermediate objects across threads should be strongly discouraged in documentation, as concurrent method calls on a shared intermediate object could lead to race conditions within the API usage itself, independent of the core MockRegistry's thread safety. Making intermediate objects immutable or internally synchronized are potential safeguards, but designing for single-threaded fluent chain execution is preferable.6. Contextual Stateful MockingBeyond simple request/response stubbing, many systems involve components behaving as state machines, where the response to an invocation depends not only on the arguments and context but also on the component's current state within that context.84 ContextualMocker can be extended to support this, drawing inspiration from features like WireMock's Scenarios.47

Use Case: Simulating components like workflow engines, order processors, or connection handlers that transition through defined states (e.g., PENDING -> PROCESSING -> COMPLETED). The behavior (e.g., methods allowed, return values) changes based on the current state, and this state needs to be maintained independently for each context (e.g., each order ID, each user session).


API Design: Extend the stubbing API to incorporate state matching and transitions:

State Identifiers: Allow users to define states using simple types like Strings or Enums.
Initial State: Define a default initial state (e.g., ContextualState.STARTED) or provide an API to set the initial state for a context.
Stateful Stubbing:
JavaContextualMocker.given(orderProcessorMock)
   .forContext(orderId1)
   .whenStateIs("PENDING") // Match current state
   .when(processor -> processor.process())
   .thenReturn(ProcessingResult.SUCCESS)
   .willSetStateTo("PROCESSING"); // Transition state upon match

ContextualMocker.given(orderProcessorMock)
   .forContext(orderId1)
   .whenStateIs("PROCESSING")
   .when(processor -> processor.ship())
   .thenReturn(ShipmentResult.OK)
   .willSetStateTo("SHIPPED");

ContextualMocker.given(orderProcessorMock)
   .forContext(orderId1)
   .whenStateIs("SHIPPED")
   .when(processor -> processor.process()) // Invalid action in this state
   .thenThrow(new IllegalStateException("Order already shipped"));


State Management API: (Optional) Methods to explicitly query or set the state for a given mock/context might be useful for setup or complex assertions. WireMock.resetAllScenarios() 47 provides a precedent for state reset.



Internal State Management:

Storage: The MockRegistry needs to store the current state for each mock instance and context ID.
Structure: A new map can be added:
Java// Key: Weak ref to mock instance, Value: Map of ContextID -> Current State
ConcurrentMap<WeakReference<Object>, ConcurrentMap<Object, AtomicReference<Object>>> currentStates;


AtomicReference<Object>: Holds the current state identifier (String, Enum, etc.) and allows for atomic updates.


Atomicity: State transitions triggered by willSetStateTo must occur atomically as part of processing the matched invocation. Using AtomicReference.compareAndSet or similar atomic operations, potentially synchronized with the invocation handling logic, is crucial to prevent race conditions where concurrent invocations might attempt to transition state simultaneously.



Invocation Handling: The ContextualInvocationHandler's logic becomes more complex:

Identify the ContextID.
Retrieve the current state for the mock/context from currentStates map (defaulting to initial state if not present).
Search stubbingRules for a rule matching the invocation arguments and the current state (whenStateIs).
If a matching rule is found:
a.  Execute the rule's Answer or prepare the return value.
b.  If the rule includes willSetStateTo, atomically update the currentStates map for this context.
c.  Return the result or throw the exception.
If no matching rule is found, apply default behavior (e.g., return null/default value).
Record the invocation in invocationRecords.


Adding stateful behavior introduces significant complexity. The interactions between concurrent invocations, state matching, and atomic state transitions require careful design and rigorous testing to ensure correctness and prevent subtle race conditions. Verification APIs might also need enhancement to support state-aware assertions (e.g., verifying calls made while in a specific state). Therefore, implementing stateful mocking might be best deferred to a post-v1.0 release, allowing the core thread-safe, context-aware functionality to stabilize first.7. Implementation ConsiderationsSeveral technical aspects require careful consideration during implementation.

Choice of Concurrency Primitives:

ConcurrentHashMap: The workhorse for the primary MockRegistry data structures. Its segmented locking (or node-based locking in later JDKs) provides good scalability for concurrent access across different mocks/contexts.53 Performance under high contention on the same key needs monitoring.55
ThreadLocal: Should only be used for the optional implicit context identification strategy. Its use must be carefully managed and documented due to risks in thread pools and potential leaks.22 It is unsuitable for storing the core mock state.
Locks (ReentrantLock, StampedLock): Necessary for operations requiring atomicity beyond ConcurrentHashMap's capabilities (e.g., complex verify logic, certain updates to rule lists if not using CopyOnWriteArrayList). Locks must be fine-grained (per mock/context entry) to avoid becoming bottlenecks.
Atomic Variables (AtomicReference, AtomicInteger etc.): Ideal for managing simple counters or the single state value per context in the stateful mocking feature.



Performance Analysis and Potential Bottlenecks:

Invocation Recording: This is likely the highest-frequency write operation. Contention on the invocation list/queue for a single, heavily mocked shared object under parallel load is a primary concern. ConcurrentLinkedQueue offers non-blocking appends, but retrieval for verification might be slower. Sharding the records (e.g., using multiple queues based on context hash) or batching writes could be explored if profiling indicates a bottleneck. Benchmarking different queue/list implementations is crucial.55
Stubbing Rule Lookup: Reading rules during invocation handling needs to be fast. ConcurrentHashMap reads are typically non-blocking and highly concurrent.55 Ensuring ContextID objects have efficient and well-distributed hashCode() implementations is important.
Mock Creation Overhead: Bytecode generation can be time-consuming.52 Implementing caching for generated mock classes, similar to Mockito's internal mechanisms, can mitigate this.
Memory Footprint: Storing every InvocationRecord can consume significant memory, especially in long-running tests or with high interaction counts. Strategies like providing configuration options to limit the history size, disable recording entirely (a context-aware stubOnly mode 90), or using more memory-efficient record representations should be considered. Using WeakReference for mock instances in the registry keys is essential to prevent leaks.



Bytecode Manipulation Library (ByteBuddy):

Selection: ByteBuddy is the recommended choice due to its widespread adoption (Mockito, Spock 65), active development, powerful features, and generally good performance.
Challenges: Integration might encounter complexities related to Java module system (JPMS) restrictions, reflection limitations, or interactions with different classloaders, although ByteBuddy often provides mechanisms to handle these.91 Requires expertise in bytecode manipulation.



API Design Principles Applied:

Fluency & Readability: Prioritize chainable methods and clear naming (e.g., given().forContext().when()...).
Explicitness over Implicitness: Default to explicit context passing for safety. Make ThreadLocal usage optional and clearly documented.
Consistency: Adapt familiar Mockito patterns (when/thenReturn, verify/times) for the contextual API.
Immutability: Design InvocationRecord and potentially StubbingRule as immutable objects to simplify concurrent access.
Fail Fast & Clear Errors: Provide informative exceptions for configuration errors, API misuse, or detected concurrency issues.



Avoiding Mockito's Internal Pitfalls:

No Core Reliance on ThreadLocal: The central MockRegistry keyed by mock/context avoids Mockito's core thread-local state issues for stubbing/verification.8
Atomic State Updates: Use appropriate concurrent collections and primitives (ConcurrentHashMap, AtomicReference, fine-grained locks) to ensure safe updates to the registry.
Decoupling: Separate the concerns of API calls (writing state) and invocation handling (reading state), ensuring thread-safe interaction with the MockRegistry.


It is critical to establish clear boundaries between the public API, Service Provider Interfaces (SPIs), and internal implementation details. Mockito's history shows that users attempting to interact with or extend internal classes face significant risks of breakage during framework upgrades.91 ContextualMocker must provide stable, well-documented extension points and strongly discourage reliance on internal implementation specifics.8. Initial Release Scope (Non-Goals)To ensure a focused and robust initial release (v1.0), ContextualMocker should prioritize solving the core problem of parallel-safe, context-aware mocking for shared instances. Features that add significant implementation complexity or deviate from this core goal should be deferred.

Rationale: A focused initial release allows for thorough testing and stabilization of the fundamental concurrency and context management mechanisms. Adding complex features incrementally is less risky than attempting everything at once, a lesson learned from Mockito's own evolution.3


Specific Non-Goals for v1.0:

Static Method Mocking: Requires complex bytecode manipulation (agents, classloader tricks) and has known pitfalls.106 Mockito added this capability much later in its lifecycle.109 Defer.
Constructor Mocking: Shares similar implementation complexities with static mocking.61 Defer.
Final Class/Method Mocking: Necessitates an "inline mock maker" approach 61, adding dependencies (Java Agent attachment) and potential runtime environment limitations (e.g., certain JVMs, Android 105). Defer.
Deep Stubbing (e.g., RETURNS_DEEP_STUBS): Introduces significant internal state management complexity and has known concurrency challenges.12 Defer.
Spying on Real Objects (spy()): Involves partial mocking and calling real methods, adding complexity beyond pure mock scenarios.28 Focus on standard mocks first.
Android Support: Requires dedicated MockMaker implementations and testing on the Android runtime/Dalvik.62 Defer.


By deferring these advanced features, the initial development can concentrate on delivering a reliable and well-tested solution for the primary use case: managing context-specific behavior for shared mock instances in concurrent test environments.9. Open Source Project FoundationEstablishing a solid foundation is crucial for the success and adoption of ContextualMocker as an open-source project.

9.1 Documentation Strategy: High-quality documentation is essential.

User Guide: A comprehensive guide covering:

Core Concepts: Explain context, concurrency safety guarantees, state management approach.
API Usage: Detailed examples for mock creation, contextual stubbing, contextual verification, and context management strategies (Explicit, ThreadLocal, Hooks). Provide clear code snippets.113
Stateful Mocking: (If included) Explain state machine concepts and API usage.
Extension Points: Document SPIs for integration.
Best Practices & Pitfalls: Guide users on effective usage and warn about potential issues (e.g., ThreadLocal usage, ContextID requirements).
Comparison: Briefly compare with Mockito/WireMock for relevant use cases.


API Reference (Javadoc): Thorough Javadoc for all public classes, methods, and interfaces. Explicitly document thread-safety guarantees or constraints for each API element. Ensure consistency between the user guide and Javadoc.82
Examples Repository: A separate repository or module with runnable examples demonstrating common patterns and integrations (JUnit 5, TestNG, common context sources).
Migration Guide: (Optional) Guidance for users migrating specific test patterns from Mockito.



9.2 Community Engagement: Foster an active and welcoming community.

Issue Tracker: Utilize GitHub Issues for transparent tracking of bugs, feature requests, and discussions.8
Mailing List/Forum: Set up a dedicated communication channel (e.g., Google Group, Discord) for questions, announcements, and broader discussions.3
Contribution Guide: Provide clear instructions on how to report issues, propose features, and contribute code or documentation, including coding standards and pull request process.3



9.3 Licensing and Governance:

License: Adopt a standard, permissive open-source license like MIT 117 or Apache 2.0 to encourage adoption and contribution.
Governance Model: Clearly define the project's decision-making process, roles (maintainers, contributors), and contribution workflow.


A critical aspect of documentation and community interaction must be clear communication about limitations. Experience with existing frameworks shows that user frustration often arises from misunderstandings about capabilities, especially concerning complex areas like thread safety 1 or parallel execution support.16 ContextualMocker's documentation must proactively address potential pitfalls: the risks of using the ThreadLocal context strategy in certain environments 22, the strict requirements for ContextID objects (equals/hashCode, immutability), performance trade-offs, and the specific non-goals of the initial release. A dedicated FAQ section, similar to Mockito's 1, and explicit warnings in Javadoc are vital for setting correct expectations and ensuring users can apply the framework effectively and safely.10. ConclusionContextualMocker addresses a critical gap in the Java testing ecosystem by providing a mocking framework explicitly designed for the challenges of testing concurrent applications with shared dependencies. Existing frameworks struggle with thread safety when stubbing or verifying shared mocks in parallel, and lack native support for defining mock behavior based on operational context (e.g., request ID, user ID).By employing a robust, thread-safe internal architecture centered around a MockRegistry using ConcurrentHashMap keyed by both mock instance and context ID, ContextualMocker aims to deliver reliable and deterministic behavior even under concurrent test execution. Its public API prioritizes explicitness and clarity for context management, offering a fluent interface for defining context-specific stubbing rules and performing context-aware verification. Optional support for stateful mocking further enhances its ability to simulate complex component behavior within specific contexts.While deferring more advanced features like static and final mocking to future releases, the initial focus on solving the core concurrency and context problem provides significant value. With comprehensive documentation, clear communication about its capabilities and limitations, and an open community model, ContextualMocker has the potential to become an essential tool for developers building and testing modern, concurrent Java systems.# contextual-mock
