
Version 1.0.3
--------------
* Flipped the GameLoop's update loop to sleep when idle instead of
    only after an actual update was run.
* Modified GameLoop to allow configuring the amount of time to sleep
    when idle-busy-polling for the next update interval.
    

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
