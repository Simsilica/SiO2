Version 1.6.0 (unreleased)
--------------
* Added a CubeSceneState for quickly adding a fully lit test scene to an application.
* Added a DefaultSceneProcessor which is basically an empty implementation of JME's
    SceneProcessor interface.
* Fixed DebugHudState to automatically resize its screen layout when the viewport
    size changes.
* Updated JME version to 3.3.  (To get the new AppState ID methods.)
* Added a JobState class and Job interface for running background jobs in a
    simple and JME-friendly way.


Version 1.5.0 (latest)
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
