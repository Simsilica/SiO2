Version 1.1.1 (unreleased)
--------------
* Fixed a SimTime initialization problem where the first tpf would be
    huge.
* Fixed an issue where EntityContainer wouldn't support nested parameterized
    types.
        

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
