// process output
        /*
        final class workerThread implements Runnable {

            private String btMsg;

            public workerThread(String msg) {
                btMsg = msg;
            }

            public void run()
            {
                sendBtMsg(btMsg);
                while(!Thread.currentThread().isInterrupted())
                {
                    int bytesAvailable;
                    boolean workDone = false;

                    try {



                        final InputStream mmInputStream;
                        mmInputStream = mmSocket.getInputStream();
                        bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {

                            byte[] packetBytes = new byte[bytesAvailable];
                            Log.e("Aquarium recv bt","bytes available");
                            byte[] readBuffer = new byte[1024];
                            mmInputStream.read(packetBytes);

                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    //The variable data now contains our full command
                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            myLabel.setText(data);
                                        }
                                    });

                                    workDone = true;
                                    break;


                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }

                            if (workDone == true){
                                mmSocket.close();
                                break;
                            }

                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }
            }
        };
        */
