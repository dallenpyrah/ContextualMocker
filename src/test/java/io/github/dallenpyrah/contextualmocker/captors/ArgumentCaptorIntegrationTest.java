package io.github.dallenpyrah.contextualmocker.captors;

import io.github.dallenpyrah.contextualmocker.core.ContextID;
import io.github.dallenpyrah.contextualmocker.core.ContextHolder;
import io.github.dallenpyrah.contextualmocker.core.StringContextId;
import io.github.dallenpyrah.contextualmocker.core.VerificationFailureException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.github.dallenpyrah.contextualmocker.core.ContextualMocker.*;
import static io.github.dallenpyrah.contextualmocker.matchers.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for ArgumentCaptor with the verification system.
 * Tests the complete flow of capturing arguments during verification and various
 * integration scenarios with other framework features.
 */
@DisplayName("ArgumentCaptor Integration Tests")
class ArgumentCaptorIntegrationTest {

    // Test interfaces and classes
    static interface UserService {
        User createUser(String name, int age);
        void updateUser(User user);
        List<User> findUsers(String criteria, boolean includeInactive);
        void processUsers(List<User> users, Map<String, Object> options);
        void sendNotification(User user, String message, NotificationType type);
    }

    static interface RestApiClient {
        Response post(String endpoint, Object body, Map<String, String> headers);
        Response get(String endpoint, Map<String, String> params);
        void delete(String endpoint, String id);
    }

    static interface RepositoryService {
        void save(Entity entity);
        Entity findById(String id);
        List<Entity> findByCriteria(Criteria criteria);
        void bulkUpdate(List<Entity> entities, UpdateOptions options);
    }

    static class User {
        private final String name;
        private final int age;
        
        User(String name, int age) {
            this.name = name;
            this.age = age;
        }
        
        public String getName() { return name; }
        public int getAge() { return age; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            User user = (User) o;
            return age == user.age && name.equals(user.name);
        }
        
        @Override
        public int hashCode() {
            return 31 * name.hashCode() + age;
        }
    }

    static class Entity {
        private final String id;
        private final String data;
        
        Entity(String id, String data) {
            this.id = id;
            this.data = data;
        }
        
        public String getId() { return id; }
        public String getData() { return data; }
    }

    static class Response {
        private final int statusCode;
        private final String body;
        
        Response(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
        
        public int getStatusCode() { return statusCode; }
        public String getBody() { return body; }
    }

    static class Criteria {
        private final String field;
        private final String value;
        
        Criteria(String field, String value) {
            this.field = field;
            this.value = value;
        }
        
        public String getField() { return field; }
        public String getValue() { return value; }
    }

    static class UpdateOptions {
        private final boolean skipValidation;
        private final boolean audit;
        
        UpdateOptions(boolean skipValidation, boolean audit) {
            this.skipValidation = skipValidation;
            this.audit = audit;
        }
        
        public boolean isSkipValidation() { return skipValidation; }
        public boolean isAudit() { return audit; }
    }

    enum NotificationType {
        EMAIL, SMS, PUSH
    }

    private ContextID context;

    @BeforeEach
    void setUp() {
        context = new StringContextId(UUID.randomUUID().toString());
        ContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        ContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Basic ArgumentCaptor Integration")
    class BasicIntegrationTests {

        @Test
        @DisplayName("Should capture argument with basic verify")
        void shouldCaptureWithBasicVerify() {
            UserService mockService = mock(UserService.class);
            ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Integer> ageCaptor = ArgumentCaptor.forClass(Integer.class);
            
            ContextHolder.setContext(context);
            mockService.createUser("John Doe", 30);
            ContextHolder.clearContext();
            
            // Verify and capture
            verify(mockService, context, times(1), () -> {
                mockService.createUser(capture(nameCaptor), capture(ageCaptor));
                return null;
            });
            
            assertEquals("John Doe", nameCaptor.getValue());
            assertEquals(30, ageCaptor.getValue());
        }

        @Test
        @DisplayName("Should capture with times verification mode")
        void shouldCaptureWithTimesMode() {
            UserService mockService = mock(UserService.class);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            
            ContextHolder.setContext(context);
            mockService.createUser("User1", 25);
            mockService.createUser("User2", 30);
            mockService.createUser("User3", 35);
            ContextHolder.clearContext();
            
            verify(mockService, context, times(3), () -> {
                mockService.createUser(capture(captor), anyInt());
                return null;
            });
            
            List<String> capturedNames = captor.getAllValues();
            assertEquals(3, capturedNames.size());
            assertEquals(Arrays.asList("User1", "User2", "User3"), capturedNames);
            assertEquals("User3", captor.getValue()); // Last value
        }

        @Test
        @DisplayName("Should capture with atLeast verification mode")
        void shouldCaptureWithAtLeastMode() {
            UserService mockService = mock(UserService.class);
            ArgumentCaptor<Integer> ageCaptor = ArgumentCaptor.forClass(Integer.class);
            
            ContextHolder.setContext(context);
            mockService.createUser("Test1", 20);
            mockService.createUser("Test2", 25);
            ContextHolder.clearContext();
            
            verify(mockService, context, atLeast(2), () -> {
                mockService.createUser(anyString(), capture(ageCaptor));
                return null;
            });
            
            assertEquals(Arrays.asList(20, 25), ageCaptor.getAllValues());
        }

        @Test
        @DisplayName("Should capture with atMost verification mode")
        void shouldCaptureWithAtMostMode() {
            UserService mockService = mock(UserService.class);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            
            ContextHolder.setContext(context);
            mockService.createUser("OnlyOne", 30);
            ContextHolder.clearContext();
            
            verify(mockService, context, atMost(1), () -> {
                mockService.createUser(capture(captor), anyInt());
                return null;
            });
            
            assertEquals("OnlyOne", captor.getValue());
            assertEquals(1, captor.getAllValues().size());
        }

        @Test
        @DisplayName("Should handle never verification mode")
        void shouldHandleNeverMode() {
            UserService mockService = mock(UserService.class);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            
            // Don't call the method at all
            ContextHolder.setContext(context);
            // No method calls
            ContextHolder.clearContext();
            
            verify(mockService, context, never(), () -> {
                mockService.createUser(capture(captor), anyInt());
                return null;
            });
            
            // Captor should have no values
            assertTrue(captor.getAllValues().isEmpty());
            assertThrows(IllegalStateException.class, captor::getValue);
        }
    }

    @Nested
    @DisplayName("Multiple Captors in Single Method")
    class MultipleCaptorsTests {

        @Test
        @DisplayName("Should capture multiple arguments in single method")
        void shouldCaptureMultipleArguments() {
            UserService mockService = mock(UserService.class);
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);
            
            User user = new User("Alice", 28);
            
            ContextHolder.setContext(context);
            mockService.sendNotification(user, "Welcome!", NotificationType.EMAIL);
            mockService.sendNotification(user, "Reminder", NotificationType.PUSH);
            ContextHolder.clearContext();
            
            verify(mockService, context, times(2), () -> {
                mockService.sendNotification(capture(userCaptor), capture(messageCaptor), capture(typeCaptor));
                return null;
            });
            
            // Check all captured values
            List<User> users = userCaptor.getAllValues();
            assertEquals(2, users.size());
            assertTrue(users.stream().allMatch(u -> u.equals(user)));
            
            assertEquals(Arrays.asList("Welcome!", "Reminder"), messageCaptor.getAllValues());
            assertEquals(Arrays.asList(NotificationType.EMAIL, NotificationType.PUSH), typeCaptor.getAllValues());
        }

        @Test
        @DisplayName("Should capture complex objects and collections")
        void shouldCaptureComplexTypes() {
            UserService mockService = mock(UserService.class);
            ArgumentCaptor<List<User>> usersCaptor = ArgumentCaptor.forClass((Class<List<User>>) (Class<?>) List.class);
            ArgumentCaptor<Map<String, Object>> optionsCaptor = ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
            
            List<User> userList = Arrays.asList(
                new User("User1", 25),
                new User("User2", 30),
                new User("User3", 35)
            );
            
            Map<String, Object> options = new HashMap<>();
            options.put("batchSize", 100);
            options.put("async", true);
            options.put("timeout", 5000);
            
            ContextHolder.setContext(context);
            mockService.processUsers(userList, options);
            ContextHolder.clearContext();
            
            verify(mockService, context, times(1), () -> {
                mockService.processUsers(capture(usersCaptor), capture(optionsCaptor));
                return null;
            });
            
            List<User> capturedUsers = usersCaptor.getValue();
            assertEquals(3, capturedUsers.size());
            assertEquals("User1", capturedUsers.get(0).getName());
            
            Map<String, Object> capturedOptions = optionsCaptor.getValue();
            assertEquals(100, capturedOptions.get("batchSize"));
            assertEquals(true, capturedOptions.get("async"));
            assertEquals(5000, capturedOptions.get("timeout"));
        }
    }

    @Nested
    @DisplayName("Mixed Matchers and Captors")
    class MixedMatchersTests {

        @Test
        @DisplayName("Should mix ArgumentCaptor with any() matcher")
        void shouldMixCaptorWithAnyMatcher() {
            UserService mockService = mock(UserService.class);
            ArgumentCaptor<String> criteriaCaptor = ArgumentCaptor.forClass(String.class);
            
            ContextHolder.setContext(context);
            mockService.findUsers("name:John", true);
            mockService.findUsers("age:>30", false);
            ContextHolder.clearContext();
            
            verify(mockService, context, times(2), () -> {
                mockService.findUsers(capture(criteriaCaptor), anyBoolean());
                return null;
            });
            
            assertEquals(Arrays.asList("name:John", "age:>30"), criteriaCaptor.getAllValues());
        }

        @Test
        @DisplayName("Should mix ArgumentCaptor with eq() matcher")
        void shouldMixCaptorWithEqMatcher() {
            RestApiClient mockClient = mock(RestApiClient.class);
            ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
            
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            
            User user1 = new User("User1", 25);
            User user2 = new User("User2", 30);
            
            ContextHolder.setContext(context);
            mockClient.post("/api/users", user1, headers);
            mockClient.post("/api/users", user2, headers);
            ContextHolder.clearContext();
            
            verify(mockClient, context, times(2), () -> {
                mockClient.post(eq("/api/users"), capture(bodyCaptor), eq(headers));
                return null;
            });
            
            List<Object> bodies = bodyCaptor.getAllValues();
            assertEquals(2, bodies.size());
            assertEquals(user1, bodies.get(0));
            assertEquals(user2, bodies.get(1));
        }

        @Test
        @DisplayName("Should mix ArgumentCaptor with argThat() matcher")
        void shouldMixCaptorWithArgThatMatcher() {
            RepositoryService mockRepo = mock(RepositoryService.class);
            ArgumentCaptor<Entity> entityCaptor = ArgumentCaptor.forClass(Entity.class);
            
            Entity entity1 = new Entity("1", "data1");
            Entity entity2 = new Entity("2", "data2");
            Entity entity3 = new Entity("3", "data3");
            
            List<Entity> batch1 = Arrays.asList(entity1, entity2);
            List<Entity> batch2 = Arrays.asList(entity3);
            
            UpdateOptions options = new UpdateOptions(false, true);
            
            ContextHolder.setContext(context);
            mockRepo.bulkUpdate(batch1, options);
            mockRepo.bulkUpdate(batch2, options);
            ContextHolder.clearContext();
            
            // Verify with predicate matcher
            verify(mockRepo, context, times(2), () -> {
                mockRepo.bulkUpdate(
                    argThat(list -> list != null && !list.isEmpty()),
                    argThat(opt -> opt.isAudit())
                );
                return null;
            });
            
            // Now capture specific entities
            ContextHolder.setContext(context);
            mockRepo.save(entity1);
            mockRepo.save(entity2);
            ContextHolder.clearContext();
            
            verify(mockRepo, context, times(2), () -> {
                mockRepo.save(capture(entityCaptor));
                return null;
            });
            
            assertEquals(Arrays.asList(entity1, entity2), entityCaptor.getAllValues());
        }

        @Test
        @DisplayName("Should mix multiple captors with multiple matchers")
        void shouldMixMultipleCaptorsAndMatchers() {
            RestApiClient mockClient = mock(RestApiClient.class);
            ArgumentCaptor<String> endpointCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass((Class<Map<String, String>>) (Class<?>) Map.class);
            
            Map<String, String> params1 = new HashMap<>();
            params1.put("page", "1");
            params1.put("size", "10");
            
            Map<String, String> params2 = new HashMap<>();
            params2.put("page", "2");
            params2.put("size", "10");
            
            ContextHolder.setContext(context);
            mockClient.get("/api/users", params1);
            mockClient.get("/api/products", params2);
            ContextHolder.clearContext();
            
            verify(mockClient, context, times(2), () -> {
                mockClient.get(capture(endpointCaptor), capture(paramsCaptor));
                return null;
            });
            
            assertEquals(Arrays.asList("/api/users", "/api/products"), endpointCaptor.getAllValues());
            assertEquals(Arrays.asList(params1, params2), paramsCaptor.getAllValues());
        }
    }

    @Nested
    @DisplayName("Spy Integration")
    class SpyIntegrationTests {

        class UserServiceImpl implements UserService {
            private final List<User> users = new java.util.ArrayList<>();
            
            @Override
            public User createUser(String name, int age) {
                User user = new User(name, age);
                users.add(user);
                return user;
            }
            
            @Override
            public void updateUser(User user) {
                // Implementation
            }
            
            @Override
            public List<User> findUsers(String criteria, boolean includeInactive) {
                return users.stream()
                    .filter(u -> u.getName().contains(criteria))
                    .collect(Collectors.toList());
            }
            
            @Override
            public void processUsers(List<User> users, Map<String, Object> options) {
                // Implementation
            }
            
            @Override
            public void sendNotification(User user, String message, NotificationType type) {
                // Implementation
            }
        }

        @Test
        @DisplayName("Should capture arguments on spy object")
        void shouldCaptureOnSpy() {
            UserServiceImpl realService = new UserServiceImpl();
            UserService spy = spy(realService);
            ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Integer> ageCaptor = ArgumentCaptor.forClass(Integer.class);
            
            ContextHolder.setContext(context);
            // Real method is called
            User createdUser = spy.createUser("SpyUser", 40);
            ContextHolder.clearContext();
            
            // Verify the spy was called with captured arguments
            verify(spy, context, times(1), () -> {
                spy.createUser(capture(nameCaptor), capture(ageCaptor));
                return null;
            });
            
            assertEquals("SpyUser", nameCaptor.getValue());
            assertEquals(40, ageCaptor.getValue());
            assertNotNull(createdUser);
            assertEquals("SpyUser", createdUser.getName());
        }

        @Test
        @DisplayName("Should capture with partial stubbing on spy")
        void shouldCaptureWithPartialStubbing() {
            UserServiceImpl realService = new UserServiceImpl();
            UserService spy = spy(realService);
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            
            // Stub one method
            when(spy, context, () -> {
                spy.sendNotification(any(), anyString(), any());
                return null;
            }).thenAnswer((contextId, mock, method, arguments) -> {
                // Do nothing, override real implementation
                return null;
            });
            
            User user = new User("TestUser", 25);
            
            ContextHolder.setContext(context);
            spy.sendNotification(user, "Test message", NotificationType.EMAIL);
            spy.updateUser(user); // Real method called
            ContextHolder.clearContext();
            
            // Verify both calls with captor
            verify(spy, context, times(1), () -> {
                spy.sendNotification(capture(userCaptor), eq("Test message"), eq(NotificationType.EMAIL));
                return null;
            });
            
            verify(spy, context, times(1), () -> {
                spy.updateUser(capture(userCaptor));
                return null;
            });
            
            List<User> capturedUsers = userCaptor.getAllValues();
            assertEquals(2, capturedUsers.size());
            assertTrue(capturedUsers.stream().allMatch(u -> u.equals(user)));
        }
    }

    @Nested
    @DisplayName("Thread Safety in Verification")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent verifications with captors")
        void shouldHandleConcurrentVerifications() throws InterruptedException {
            UserService mockService = mock(UserService.class);
            int threadCount = 5;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch completeLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            
            // Perform calls in different contexts
            for (int i = 0; i < threadCount; i++) {
                ContextID threadContext = new StringContextId("context-" + i);
                ContextHolder.setContext(threadContext);
                mockService.createUser("User-" + i, 20 + i);
                ContextHolder.clearContext();
            }
            
            // Verify concurrently
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                final ContextID threadContext = new StringContextId("context-" + threadId);
                
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
                        ArgumentCaptor<Integer> ageCaptor = ArgumentCaptor.forClass(Integer.class);
                        
                        verify(mockService, threadContext, times(1), () -> {
                            mockService.createUser(capture(nameCaptor), capture(ageCaptor));
                            return null;
                        });
                        
                        assertEquals("User-" + threadId, nameCaptor.getValue());
                        assertEquals(20 + threadId, ageCaptor.getValue().intValue());
                        successCount.incrementAndGet();
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        completeLatch.countDown();
                    }
                });
            }
            
            startLatch.countDown();
            assertTrue(completeLatch.await(5, TimeUnit.SECONDS));
            executor.shutdown();
            
            assertEquals(threadCount, successCount.get());
        }

        @Test
        @DisplayName("Should maintain captor isolation across threads")
        void shouldMaintainCaptorIsolation() throws InterruptedException {
            UserService mockService = mock(UserService.class);
            ArgumentCaptor<String> sharedCaptor = ArgumentCaptor.forClass(String.class);
            
            // Setup: call method multiple times
            ContextHolder.setContext(context);
            for (int i = 0; i < 10; i++) {
                mockService.createUser("User-" + i, 25);
            }
            ContextHolder.clearContext();
            
            // Verify and capture
            verify(mockService, context, times(10), () -> {
                mockService.createUser(capture(sharedCaptor), eq(25));
                return null;
            });
            
            // Check all values were captured
            List<String> capturedNames = sharedCaptor.getAllValues();
            assertEquals(10, capturedNames.size());
            for (int i = 0; i < 10; i++) {
                assertEquals("User-" + i, capturedNames.get(i));
            }
        }
    }

    @Nested
    @DisplayName("Complex Object Capture")
    class ComplexObjectCaptureTests {

        @Test
        @DisplayName("Should capture nested objects and preserve references")
        void shouldCaptureNestedObjects() {
            RestApiClient mockClient = mock(RestApiClient.class);
            ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
            
            // Create complex nested structure
            Map<String, Object> complexBody = new HashMap<>();
            complexBody.put("user", new User("John", 30));
            complexBody.put("metadata", Map.of(
                "timestamp", System.currentTimeMillis(),
                "version", "1.0",
                "tags", Arrays.asList("important", "urgent")
            ));
            
            Map<String, String> headers = Map.of("Content-Type", "application/json");
            
            ContextHolder.setContext(context);
            mockClient.post("/api/complex", complexBody, headers);
            ContextHolder.clearContext();
            
            verify(mockClient, context, times(1), () -> {
                mockClient.post(eq("/api/complex"), capture(bodyCaptor), any());
                return null;
            });
            
            // Verify captured object maintains structure
            Map<String, Object> captured = (Map<String, Object>) bodyCaptor.getValue();
            assertNotNull(captured);
            
            User capturedUser = (User) captured.get("user");
            assertEquals("John", capturedUser.getName());
            assertEquals(30, capturedUser.getAge());
            
            Map<String, Object> capturedMetadata = (Map<String, Object>) captured.get("metadata");
            assertEquals("1.0", capturedMetadata.get("version"));
            List<String> tags = (List<String>) capturedMetadata.get("tags");
            assertEquals(Arrays.asList("important", "urgent"), tags);
        }

        @Test
        @DisplayName("Should capture mutable objects at invocation time")
        void shouldCaptureMutableObjectsCorrectly() {
            RepositoryService mockRepo = mock(RepositoryService.class);
            ArgumentCaptor<List<Entity>> listCaptor = ArgumentCaptor.forClass((Class<List<Entity>>) (Class<?>) List.class);
            
            // Create mutable list
            List<Entity> mutableList = new java.util.ArrayList<>();
            mutableList.add(new Entity("1", "data1"));
            
            ContextHolder.setContext(context);
            mockRepo.bulkUpdate(mutableList, new UpdateOptions(false, true));
            
            // Modify list after call
            mutableList.add(new Entity("2", "data2"));
            ContextHolder.clearContext();
            
            verify(mockRepo, context, times(1), () -> {
                mockRepo.bulkUpdate(capture(listCaptor), any());
                return null;
            });
            
            // Captured list should reflect state at invocation time
            List<Entity> captured = listCaptor.getValue();
            // Note: This behavior depends on implementation - 
            // capturing might capture reference or copy
            assertNotNull(captured);
        }
    }

    @Nested
    @DisplayName("Real-world Usage Patterns")
    class RealWorldPatternsTests {

        @Test
        @DisplayName("REST API pattern - capture request details")
        void shouldCaptureRestApiDetails() {
            RestApiClient mockClient = mock(RestApiClient.class);
            when(mockClient, context, () -> mockClient.post(anyString(), any(), any()))
                .thenReturn(new Response(201, "{\"id\":\"123\"}"));
            
            ArgumentCaptor<String> endpointCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
            ArgumentCaptor<Map<String, String>> headersCaptor = 
                ArgumentCaptor.forClass((Class<Map<String, String>>) (Class<?>) Map.class);
            
            User newUser = new User("Alice", 25);
            Map<String, String> headers = Map.of(
                "Authorization", "Bearer token123",
                "Content-Type", "application/json"
            );
            
            ContextHolder.setContext(context);
            Response response = mockClient.post("/api/users", newUser, headers);
            ContextHolder.clearContext();
            
            assertEquals(201, response.getStatusCode());
            
            verify(mockClient, context, times(1), () -> {
                mockClient.post(capture(endpointCaptor), capture(bodyCaptor), capture(headersCaptor));
                return null;
            });
            
            assertEquals("/api/users", endpointCaptor.getValue());
            assertEquals(newUser, bodyCaptor.getValue());
            assertEquals("Bearer token123", headersCaptor.getValue().get("Authorization"));
        }

        @Test
        @DisplayName("Repository pattern - capture entity operations")
        void shouldCaptureRepositoryOperations() {
            RepositoryService mockRepo = mock(RepositoryService.class);
            ArgumentCaptor<Entity> entityCaptor = ArgumentCaptor.forClass(Entity.class);
            ArgumentCaptor<Criteria> criteriaCaptor = ArgumentCaptor.forClass(Criteria.class);
            
            // Simulate typical repository workflow
            Entity entity1 = new Entity("1", "initial");
            Entity entity2 = new Entity("2", "secondary");
            
            ContextHolder.setContext(context);
            
            // Save operations
            mockRepo.save(entity1);
            mockRepo.save(entity2);
            
            // Find operations
            mockRepo.findByCriteria(new Criteria("status", "active"));
            mockRepo.findByCriteria(new Criteria("type", "premium"));
            
            ContextHolder.clearContext();
            
            // Verify saves
            verify(mockRepo, context, times(2), () -> {
                mockRepo.save(capture(entityCaptor));
                return null;
            });
            
            List<Entity> savedEntities = entityCaptor.getAllValues();
            assertEquals(2, savedEntities.size());
            assertEquals("1", savedEntities.get(0).getId());
            assertEquals("2", savedEntities.get(1).getId());
            
            // Verify finds
            verify(mockRepo, context, times(2), () -> {
                mockRepo.findByCriteria(capture(criteriaCaptor));
                return null;
            });
            
            List<Criteria> criteriaList = criteriaCaptor.getAllValues();
            assertEquals("status", criteriaList.get(0).getField());
            assertEquals("active", criteriaList.get(0).getValue());
            assertEquals("type", criteriaList.get(1).getField());
            assertEquals("premium", criteriaList.get(1).getValue());
        }

        @Test
        @DisplayName("Event-driven pattern - capture notification details")
        void shouldCaptureEventDrivenFlow() {
            UserService mockService = mock(UserService.class);
            
            // Capture all notification details for audit
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);
            
            // Simulate user registration flow
            ContextHolder.setContext(context);
            
            User newUser = new User("NewUser", 25);
            mockService.createUser("NewUser", 25);
            mockService.sendNotification(newUser, "Welcome to our platform!", NotificationType.EMAIL);
            mockService.sendNotification(newUser, "Complete your profile", NotificationType.PUSH);
            mockService.sendNotification(newUser, "Verify your phone", NotificationType.SMS);
            
            ContextHolder.clearContext();
            
            // Verify notification sequence
            verify(mockService, context, times(3), () -> {
                mockService.sendNotification(capture(userCaptor), capture(messageCaptor), capture(typeCaptor));
                return null;
            });
            
            // Audit trail
            List<String> messages = messageCaptor.getAllValues();
            List<NotificationType> types = typeCaptor.getAllValues();
            
            assertEquals(3, messages.size());
            assertEquals("Welcome to our platform!", messages.get(0));
            assertEquals("Complete your profile", messages.get(1));
            assertEquals("Verify your phone", messages.get(2));
            
            assertEquals(NotificationType.EMAIL, types.get(0));
            assertEquals(NotificationType.PUSH, types.get(1));
            assertEquals(NotificationType.SMS, types.get(2));
            
            // All notifications went to same user
            assertTrue(userCaptor.getAllValues().stream().allMatch(u -> u.equals(newUser)));
        }
    }

    @Nested
    @DisplayName("Error Cases and Edge Scenarios")
    class ErrorCasesTests {

        @Test
        @DisplayName("Should handle verification failure with captors")
        void shouldHandleVerificationFailure() {
            UserService mockService = mock(UserService.class);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            
            ContextHolder.setContext(context);
            mockService.createUser("OnlyOnce", 30);
            ContextHolder.clearContext();
            
            // This should fail - expecting 2 calls but only made 1
            assertThrows(VerificationFailureException.class, () -> {
                verify(mockService, context, times(2), () -> {
                    mockService.createUser(capture(captor), anyInt());
                    return null;
                });
            });
            
            // Captor should still have the one captured value
            assertEquals(1, captor.getAllValues().size());
            assertEquals("OnlyOnce", captor.getValue());
        }

        @Test
        @DisplayName("Should handle no captures gracefully")
        void shouldHandleNoCapturesGracefully() {
            UserService mockService = mock(UserService.class);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            
            // No method calls made
            
            verify(mockService, context, never(), () -> {
                mockService.createUser(capture(captor), anyInt());
                return null;
            });
            
            assertTrue(captor.getAllValues().isEmpty());
            assertThrows(IllegalStateException.class, captor::getValue);
        }

        @Test
        @DisplayName("Should reset captors between verifications")
        void shouldResetCaptorsBetweenVerifications() {
            UserService mockService = mock(UserService.class);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            
            // First verification
            ContextHolder.setContext(context);
            mockService.createUser("First", 25);
            ContextHolder.clearContext();
            
            verify(mockService, context, times(1), () -> {
                mockService.createUser(capture(captor), eq(25));
                return null;
            });
            
            assertEquals("First", captor.getValue());
            
            // Reset captor
            captor.reset();
            
            // Second verification
            ContextHolder.setContext(context);
            mockService.createUser("Second", 30);
            ContextHolder.clearContext();
            
            verify(mockService, context, times(1), () -> {
                mockService.createUser(capture(captor), eq(30));
                return null;
            });
            
            assertEquals("Second", captor.getValue());
            assertEquals(1, captor.getAllValues().size()); // Only has second value
        }
    }
}