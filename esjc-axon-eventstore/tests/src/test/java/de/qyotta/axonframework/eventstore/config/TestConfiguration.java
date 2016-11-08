package de.qyotta.axonframework.eventstore.config;

import de.qyotta.axonframework.eventstore.EsjcEventStore;
import de.qyotta.axonframework.eventstore.domain.MyTestAggregate;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandTargetResolver;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.commandhandling.annotation.AggregateAnnotationCommandHandler;
import org.axonframework.commandhandling.annotation.AnnotationCommandHandlerBeanPostProcessor;
import org.axonframework.commandhandling.annotation.AnnotationCommandTargetResolver;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.commandhandling.gateway.CommandGatewayFactoryBean;
import org.axonframework.commandhandling.gateway.IntervalRetryScheduler;
import org.axonframework.commandhandling.gateway.RetryScheduler;
import org.axonframework.common.annotation.DefaultParameterResolverFactory;
import org.axonframework.common.annotation.MultiParameterResolverFactory;
import org.axonframework.common.annotation.ParameterResolverFactory;
import org.axonframework.common.annotation.SpringBeanParameterResolverFactory;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.SimpleEventBus;
import org.axonframework.eventhandling.annotation.AnnotationEventListenerBeanPostProcessor;
import org.axonframework.eventsourcing.EventSourcingRepository;
import org.axonframework.eventstore.EventStore;
import org.axonframework.saga.ResourceInjector;
import org.axonframework.saga.spring.SpringResourceInjector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.github.msemys.esjc.EventStoreBuilder;

@Configuration
@ComponentScan(basePackages = { "de.qyotta.axonframework.eventstore.config" })
public class TestConfiguration {

   @Bean
   public com.github.msemys.esjc.EventStore eventStoreClient() {
      final EventStoreBuilder eventStoreBuilder = EventStoreBuilder.newBuilder()
            .clientReconnectionDelay(Duration.ofSeconds(3))
            .userCredentials("admin", "changeit")
            .clusterNodeUnlimitedDiscoverAttempts()
            .unlimitedClientReconnections()
            .unlimitedOperationRetries()
            .singleNodeAddress("127.0.0.1", 3334)
            .requireMasterEnabled();

      return eventStoreBuilder.build();
   }

   @Bean
   @Autowired
   public EventStore eventStore(final com.github.msemys.esjc.EventStore eventStoreClient) {
      return new EsjcEventStore(eventStoreClient);
   }

   @Bean(name = "testAggregateEventsourcingRepository")
   @Autowired
   public EventSourcingRepository<MyTestAggregate> respository(final EventStore eventStore, final EventBus eventBus) {
      final EventSourcingRepository<MyTestAggregate> repository = new EventSourcingRepository<>(MyTestAggregate.class, eventStore);
      repository.setEventBus(eventBus);
      return repository;
   }

   @Bean(name = "testAggregateCommandHandler")
   @Autowired
   public AggregateAnnotationCommandHandler<MyTestAggregate> commandHandler(@Qualifier("localSegment") final CommandBus commandBus, final ParameterResolverFactory parameterResolverFactory,
         @Qualifier("testAggregateEventsourcingRepository") final EventSourcingRepository<MyTestAggregate> respository) {

      final CommandTargetResolver commandTargetResolver = new AnnotationCommandTargetResolver();
      final AggregateAnnotationCommandHandler<MyTestAggregate> commandHandler = new AggregateAnnotationCommandHandler<>(MyTestAggregate.class, respository, commandTargetResolver,
            parameterResolverFactory);

      for (final String supportedCommand : commandHandler.supportedCommands()) {
         commandBus.subscribe(supportedCommand, commandHandler);
      }
      return commandHandler;
   }

   @Bean
   public ResourceInjector resourceInjector() {
      return new SpringResourceInjector();
   }

   @Bean
   @Autowired
   public ParameterResolverFactory parameterResolverFactory(final ApplicationContext applicationContext) {
      final SpringBeanParameterResolverFactory springBeanParameterResolverFactory = new SpringBeanParameterResolverFactory();
      springBeanParameterResolverFactory.setApplicationContext(applicationContext);
      return new MultiParameterResolverFactory(new DefaultParameterResolverFactory(), springBeanParameterResolverFactory);
   }

   @Bean
   @Autowired
   public AnnotationEventListenerBeanPostProcessor annotationEventListenerBeanPostProcessor(final EventBus eventBus) {
      final AnnotationEventListenerBeanPostProcessor processor = new AnnotationEventListenerBeanPostProcessor();
      processor.setEventBus(eventBus);
      return processor;
   }

   @Bean
   @Autowired
   public AnnotationCommandHandlerBeanPostProcessor annotationCommandHandlerBeanPostProcessor(final CommandBus commandBus) throws UnknownHostException {
      final AnnotationCommandHandlerBeanPostProcessor processor = new AnnotationCommandHandlerBeanPostProcessor();
      processor.setCommandBus(commandBus);
      return processor;
   }

   @Bean(name = "commandBusTaskExecutor")
   public Executor commandBusTaskExecutor() {
      final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
      threadPoolTaskExecutor.setCorePoolSize(2);
      threadPoolTaskExecutor.setMaxPoolSize(2);
      threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
      return threadPoolTaskExecutor;
   }

   @Bean
   @Autowired
   public CommandGatewayFactoryBean<CommandGateway> commandGatewayFactoryBean(final CommandBus commandBus) {
      final CommandGatewayFactoryBean<CommandGateway> factory = new CommandGatewayFactoryBean<>();
      factory.setCommandBus(commandBus);
      return factory;
   }

   @Bean
   @Autowired
   public RetryScheduler retryScheduler(final ScheduledExecutorService scheduledExecutorService) {
      return new IntervalRetryScheduler(scheduledExecutorService, 10, 1);
   }

   @Bean
   public ScheduledExecutorService scheduledExecutorService() {
      return new ScheduledThreadPoolExecutor(5);
   }

   @Bean(name = "clusterTaskExecutor")
   public Executor clusterTaskExecutor() {
      final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
      threadPoolTaskExecutor.setCorePoolSize(20);
      threadPoolTaskExecutor.setMaxPoolSize(20);
      threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
      return threadPoolTaskExecutor;
   }

   @Bean(name = "localSegment")
   public CommandBus commandBus() {
      return new SimpleCommandBus();
   }

   @Bean
   public EventBus eventBus() {
      return new SimpleEventBus();
   }
}
