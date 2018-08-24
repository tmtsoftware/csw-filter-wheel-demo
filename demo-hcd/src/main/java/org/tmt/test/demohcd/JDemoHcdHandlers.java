//package org.tmt.test.demohcd;
//
//import akka.actor.typed.javadsl.ActorContext;
//import csw.framework.CurrentStatePublisher;
//import csw.framework.javadsl.JComponentHandlers;
//import csw.messages.TopLevelActorMessage;
//import csw.messages.commands.CommandResponse;
//import csw.messages.commands.ControlCommand;
//import csw.messages.framework.ComponentInfo;
//import csw.messages.location.TrackingEvent;
//import csw.services.command.CommandResponseManager;
//import csw.services.event.api.javadsl.IEventService;
//import csw.services.location.javadsl.ILocationService;
//import csw.services.logging.javadsl.ILogger;
//import csw.services.logging.javadsl.JLoggerFactory;
//
//import java.util.concurrent.CompletableFuture;
//
///**
// * Domain specific logic should be written in below handlers.
// * This handlers gets invoked when component receives messages/commands from other component/entity.
// * For example, if one component sends Submit(Setup(args)) command to DemoHcd,
// * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
// * and if validation is successful, then onSubmit hook gets invoked.
// * You can find more information on this here : https://tmtsoftware.github.io/csw-prod/framework.html
// */
//public class JDemoHcdHandlers extends JComponentHandlers {
//
//    private ILogger log;
//    private CommandResponseManager commandResponseManager;
//    private CurrentStatePublisher currentStatePublisher;
//    private ActorContext<TopLevelActorMessage> actorContext;
//    private ILocationService locationService;
//    private IEventService eventService;
//    private ComponentInfo componentInfo;
//
//    JDemoHcdHandlers(
//          ActorContext<TopLevelActorMessage> ctx,
//          ComponentInfo componentInfo,
//          CommandResponseManager commandResponseManager,
//          CurrentStatePublisher currentStatePublisher,
//          ILocationService locationService,
//          IEventService eventService,
//          JLoggerFactory loggerFactory
//    ) {
//        super(ctx, componentInfo, commandResponseManager, currentStatePublisher, locationService, eventService, loggerFactory);
//        this.currentStatePublisher = currentStatePublisher;
//        this.log = loggerFactory.getLogger(getClass());
//        this.commandResponseManager = commandResponseManager;
//        this.actorContext = ctx;
//        this.locationService = locationService;
//        this.eventService = eventService;
//        this.componentInfo = componentInfo;
//    }
//
//    @Override
//    public CompletableFuture<Void> jInitialize() {
//        return CompletableFuture.runAsync(() -> {
//
//        });
//    }
//
//    @Override
//    public CompletableFuture<Void> jOnShutdown() {
//        return CompletableFuture.runAsync(() -> {
//
//        });
//    }
//
//    @Override
//    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {
//
//    }
//
//    @Override
//    public CommandResponse validateCommand(ControlCommand controlCommand) {
//        return null;
//    }
//
//    @Override
//    public void onSubmit(ControlCommand controlCommand) {
//
//    }
//
//    @Override
//    public void onOneway(ControlCommand controlCommand) {
//
//    }
//
//    @Override
//    public void onGoOffline() {
//
//    }
//
//    @Override
//    public void onGoOnline() {
//
//    }
//}
