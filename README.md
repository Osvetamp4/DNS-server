We had three major functions:
Determine response
handle dns lookup
process socket response


With determine response, the idea was to cleanly seperate functionality for the authoritative domain vs dns recursive lookups

If we were to do dns recursive lookups we would create a new temp socket for that and add it to a growing dictionary of those sockets.
From the very beginning we developed the code with parallel processing in mind so we didn't have to alter the code much once we got to that point on the implementation strategy section.

Within determine response, handle dns lookup would "pin" the socket to a dictionary that would then be added to the select.select list in run(), essentially forcing the dns server to also monitor that temp socket if it had any responses.

Process socket response would be called on every temp socket response that wasn't the main self.socket for clean isolated communication to the recursive dns servers.

What I think is good design, especially for parallel processing, is the structure of adding tasks to atask board that a seperate worker would go by and "complete" the tasks. IT is a simple model to code by and it greatly boosts the efficiency of the project in general. With each socket added to a dictionary, there would be a jsonlike object of values associated with that temp socket's progress in getting the client information, we were able to at our discretion append any new or pertinent data to help with that particular socket's task. This gave us a massive amount of flexibility to sort of change and adapt our approach to any sort of situation that the tests would through at us because each specific jsonlike object was isolated and we could change our behavior based on the previous information in that object, we didnt need to change behavior on a wide scale that affected every other temp socket because everything was so isolated.