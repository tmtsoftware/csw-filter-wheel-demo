//package org.tmt.test.demohcd;
//
//import akka.actor.typed.javadsl.ActorContext;
//import csw.framework.CurrentStatePublisher;
//import csw.framework.javadsl.JComponentBehaviorFactory;
//import csw.framework.javadsl.JComponentHandlers;
//import csw.messages.TopLevelActorMessage;
//import csw.messages.framework.ComponentInfo;
//import csw.services.command.CommandResponseManager;
//import csw.services.event.api.javadsl.IEventService;
//import csw.services.location.javadsl.ILocationService;
//import csw.services.logging.javadsl.JLoggerFactory;
//
//public class JDemoHcdBehaviorFactory extends JComponentBehaviorFactory {
//
//    @Override
//    public JComponentHandlers jHandlers(
//            ActorContext<TopLevelActorMessage> ctx,
//            ComponentInfo componentInfo,
//            CommandResponseManager commandResponseManager,
//            CurrentStatePublisher currentStatePublisher,
//            ILocationService locationService,
//            IEventService eventService,
//            JLoggerFactory loggerFactory
//    ) {
//        return new JDemoHcdHandlers(ctx, componentInfo, commandResponseManager, currentStatePublisher, locationService, eventService, loggerFactory);
//    }
//
//}
