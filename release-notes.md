## Version 1.0.7
- Now the reason for rejected shares is shown in log
- Added block number and miner identifier (**MMvX.X.X**) to the shares and pool connection
- Now the app will stop trying to connect to the pool in case of invalid connection and will report it to the log
- Miner sync time delayed 6 seconds (606 sec in total per block) in order to prevent unneeded connections to pool