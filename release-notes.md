Version 1.9.0 (unreleased)
--------------
* Added player joined/left log messages to ChatHostedService.
* Added an EventAbortedException and suppoet to EventBus for handling
    it as a way to stop delivery for a specific event.
* Added watch/unwatch methods to the Blackboard and BlackboardState to
    allow more easily watching specific blackboard properties.
* Modified BlackboardState to support a state ID to make it easier to
    have multiple blackboards in an application.
* Added EntityContainer.isStarted() to be able to determine if a container
    has already been started or not.
* Added an IterationProcessorThread class and IterationProcessor interface
    for managing lock-step background processes.
* Modified EntityUpdater to optionally use an IterationProcessorThread
    to perform network entity updates on a background thread.
* Added some performance monitoring to EntityUpdater to log a warning when
    an update takes a lot longer than expected.
* Updated GameSystemManager to set the current frame number as a "frame"
    logging MDC so that certain logging implementations can include it as
    part of the log message format.
* Added a SystemTiming class that can optionally be given to GameSystemManager
    to track per-system timings and dump the information as a warning when
    a frame exceeds a certain threshold.
* Fixed an NPE when stopping an EntityContainer that did not fully start.
* Modified GameSystemManager initalize() and start() methods to cleanup
    partial init and partial startup+init on failure.
* Modified EntityContainer's element type detection to better handle
    parameterized extensions of EntityContainer. (Complete with unit tests.)
* Added an EntityContainer.getComponentTypes() protected method so that subclasses
    can query the component types.
* Increased base guava version to 21 to get java.util.function compatibility.
* Updated MessageState to allow for a configurable max width that will be applied
    to created Labels. (so they autowrap at max width)
* Added a bunch of trace logging to the Blackboard class.
* Modified GameSystemManager stop() and terminate() to have exception handling so
    that one bad system does not (necessarily) prevent the next ones from getting
    stopped/terminated. This could lead to application hangs if later systems could
    not properly shut down their threads.  Subclasses can override the onStopError()
    or onTerminateError() methods to control the behavior, which by default logs
    and moves on.
* Added GameLoop.setPriority() for adjusting the priority of the game loop thread.
* Fixed an issue where registering the same system twice using different keys would
    also cause it to be executed twice.
* Added GameSystemManager.getSystems() for debugging code to be able to iterate
    over the active systems.
* Upped the Zay-es versions to 1.7.0-SNAPSHOT
* Updated EntityContainer to work with EntityCriteria objects.
    Possible breaking change: EntityContainer.getComponentTypes() return type has changed.
* Modified CommandConsoleState to be able to provide a VersionedReference for its enabled
    state.
* Modified MessageState to support scroll back and different font scale.


Version 1.8.0 (latest)
--------------
* Change the target version of the project to Java 8 to match the few
    Java 8 classes that are used.
* Added @SafeVarargs to EntityContainer to avoid unchecked varargs warnings.
* Added ChatHostedService.add/removeChatSessionListener() so that the server
    can listen for chat messages.
* Added ChatHostedService.postMessage() so that a server console can post
    messages to the chat.
* Added some alternate constructors to GameSystemState that allow passing
    of an existing/custom GameSystemManager.
* Added GameSystemsState.getGameSystemManager() and
    GameSystemsState.get(class, boolean) methods.
* Added RecurringTaskSystem for executing general tasks once per frame.
* Added JobState/WorkerPool.isBusy() to indicate if there are still pending/running
    workers.
* Added JobState.getPoolSize() for querying the number of threads in the pool.
* Modified JobState.getQueuedCount()/getActiveCount() to return the live numbers
    from WorkerPool instead of the (likely delayed) numbers that the versioned
    objects are tracking.
* Fixed an issue where shutdown workers were spewing InterruptedException stack
    traces to the logs during shutdown.
* Changed the element ID for MessageState labels to be "console.message.label"
    instead of "message.label".  The latter was too generic in styling and was
    often catching optionPanel.message.label as well.
* Fixed BulletSystem collision listener iteration to use getArray() instead of
    creating an iterator every time. (thanks Ali-RS)


Version 1.7.0
--------------
* Fixed a bug in MovementState where disabled states would be auto-enabled if the
    default camera initialization ran.
* Added a more general WorkerPool class to encapsulate the job management of
    JobState.  Modified JobState to delegate to it.
* Added a Blackboard class and auto-registered it with the GameSystemsManager.
* Added a BlackboardState class for convenient access to a blackboard object
    in client-side code.
* Renamed the JobState.getQueuedCountReference() and getActiveCountReference()
    to getQueuedCount() and getActiveCount() respectively.  The old one was a typo
    and since this is a newish class, hopefully it doesn't affect too many folks
    to fix it now.
* Breaking change to the CommandEntry interface where runCommand() now has a
    boolean return value.  Returning true allows the command to keep the console
    open.
* Fixed DecaySystem to call the "fail on error" version of getSystem() when looking
    up the entity system.
* Updated SimTime to have a setCurrentTime() method that allows resetting the simulation
    to a particular time, either because the game is being reloaded or because it's
    being continued after a pause, etc..
* Refactored SimTime's internals to make time tracking easier and more flexible.
* Updated GameLoop to call setCurrentTime() on its safe sim time instead of update(),
    ie: the time returned by GameLoop.getStepTime()
    This avoids a base time discrepancy but leaves a breaking change that tpf will
    always be 0.  (tpf was generally nonsense before.)
* Updated GameLoop to allow waiting for start() to complete.  If waiting then
    if there is an error during startup then it will be wrapped and rethrown
    from the start(true) method.
* Added SimEvent.simFailed and modified GameSystemsManager to publish this event
    if either initialize() or start() fails.
* Fixed a bug in MovementState where the provided walk speed wasn't being adopted
    unless the run state changed.
* Migrated the build to gradle 7.4.2
* Moved distribution to maven central.


Version 1.6.0
--------------
* Added a CubeSceneState for quickly adding a fully lit test scene to an application.
* Added a DefaultSceneProcessor which is basically an empty implementation of JME's
    SceneProcessor interface.
* Fixed DebugHudState to automatically resize its screen layout when the viewport
    size changes.
* Updated JME version to 3.3.  (To get the new AppState ID methods.)
* Added a JobState class and Job interface for running background jobs in a
    simple and JME-friendly way.
* Added some standard simple chat service classes for networked apps.
* Added CompositeAppState.clearChildren()
* Modified MovementState to handle null movement targets. The state will be
    automatically enabled/disabled with non-null/null movement targets, respectively.


Version 1.5.0
--------------
* Added an initialize and terminate methods to MovementTarget for the MovementState to
    call when the target is set.
* Added an AbstractMovementTarget class to shield subclasses from future interface
    changes.


Version 1.4.0
--------------
* Fixed a an issue where EntityContainer.stop() wasn't clearing the internal entity set
    reference causing other issues if operations were performed on the container after
    stop.
* Refactored GameLoop to allow setting a custom loop sleep strategy.
* Added a NanoLoopSleepStrategy that uses LockSupport.parkNanos() to sleep and may
    perform better on some OSes.
* Added a standard MovementState with default mappings for mouse, keyboard, and joystick
    and a default wiring to the standard JME camera. The movement target is configurable.


Version 1.3.0
--------------
* Upped the Zay-ES-net version to 1.4.0 to get the entity set filtering
    bug fix.
* CompositeAppState modified to use SafeArrayList.getArray() in for loops.
* Modified GameLoop's default frame interval constant to be public: GameLoop.FPS_60
* Added a GameSystemsState utility for managing a GameSystemManager in
    single player games.
* Added a MemoryDebugState utility that displays memory stats using the DebugHudState.
* Upped Lemur and Zay-ES/Zay-ES-Net dependencies to latest versions (1.12.0 and 1.3.1/1.4.0 respectively.)


Version 1.2.0
--------------
* Fixed a SimTime initialization problem where the first tpf would be
    huge.
* Fixed an issue where EntityContainer wouldn't support nested parameterized
    types.
* Added GameSystemManager.getStepTime()
* Added GameLoop.getStepTime() that is thread-safe with respect to the game loop
    thread.
* Added SimTime.toSimTime() and SimTime.getFutureTime(duration) to help with
    conversion of seconds to game time.
* Added a standard es.common.Decay component for tracking the 'life' of an
    entity.
* Added a standard sim.common.DecaySystem for automatically destroying entities
    whose Decay time has expired.
* Added GameSystemManager.get(class, failOnMiss) that can optionally throw an
    exception if the system does not exist.
* Added AbstractGameSystem.getSystem(class, failOnMiss) that can optionally
    throw an exception if the system does not exist.
* Added a MessageState that can be used to display fading messages popping up from
    the bottom of the screen.
* Added a basic CommandConsoleState that can be used to allow command entry (can
    automatically feed the MessageState).
* Modified DebugHudState to allow user-supplied VersionedObjects to be displayed
    instead of forcing them to be created through the API.
* Added a CameraState to make it easier to independently set camera parameters like
    FOV, near, and far plane values.
* Modified SimTime to initialize baseTime to current time on the first frame so
    that game time begins counting from the first frame.
* Fixed an NPE in CompositeAppState.addChild() when called after the outer state is
    attached but before it was initialized.  See PR #5.
* Added SimTime.getUnlockedTime() which will return a SimTime-translated time
    between frames, ie: not frame locked.
* Set sourceCompatibility to 1.7 and turned on detailed 'unchecked' warnings
* Suppressed 'unchecked' warnings in some methods of EntityContainer, EventBus,
    and DecaySystem where it is known that the operations are safe or safe-ish,
    ie: we know we did it for a reason and don't need to be reminded all the time.
* Increased Zay-ES versions to 1.3.0


Version 1.1.0
--------------
* Fixed the EventBus addListener()/removeListener() methods to be static
    like they were supposed to be.
* Added the ability to add/remove global 'dispatch' listeners to the
    EventBus.  This is useful for things like lifecycle logging or
    other debug/status related operations.
* Fixed the EventBus to properly check superclasses for autowired
    event listener methods.
* Exposed the DebugHudState's element IDs to make it easier to restyle
    the debug HUD.


Version 1.0.3
--------------
* Flipped the GameLoop's update loop to sleep when idle instead of
    only after an actual update was run.
* Modified GameLoop to allow configuring the amount of time to sleep
    when idle-busy-polling for the next update interval.
* Modified GameSystemManager to log its update errors before sending
    them to the event bus as a fatal error.
* Modified the EventBus to log.debug() any events that are undelivered.
* Modified the build.gradle to replace the JME version with a specific
    version instead of letting it float.  I think alpha4 is generally
    the minimum accepted 3.1 version at this point.
    Did the same for all of the floating version references.

Version 1.0.2
--------------
* Added some lifecycle trace logging to GameSystemManager.
* Added a DebugHudState.removeDebugValue() for removing previously created debug values
    from the HUD.
* Fixed the DebugHudState to clear the background of the "screen" container regardless
    of style settings.
* Fixed a bug where the wrong class was being reported in the exception message for
    adding event bus listeners.


Version 1.0.1
--------------
* Initial public release with maven artifacts
