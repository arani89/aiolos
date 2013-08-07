/*
 * Copyright (c) 2014, Tim Verbelen
 * Internet Based Communication Networks and Services research group (IBCN),
 * Department of Information Technology (INTEC), Ghent University - iMinds.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *    - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    - Neither the name of Ghent University - iMinds, nor the names of its 
 *      contributors may be used to endorse or promote products derived from 
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
package be.iminds.aiolos.monitor.node;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import org.osgi.service.log.LogService;

import be.iminds.aiolos.monitor.node.api.NodeMonitor;
import be.iminds.aiolos.monitor.node.api.NodeMonitorInfo;

public class NodeMonitorImpl implements NodeMonitor {
	private int interval = 1;

	private final String nodeId;
	
	// cpu
	private volatile double cpu = 0;
	private long cpuWork = 0;
	private long cpuIdle = 0;
	private long cpuWorkLast = 0;
	private long cpuIdleLast = 0;

	private int noCores = -1;

	// memory
	private long memTotal = 0;
	private long memFree = 0;
	private long memBuffers = 0;
	private long memCached = 0;
	private volatile double memory = 0;
	
	// bandwidth
	private String netinterface = null; // network interface to monitor
	private volatile long bpsIn = 0;
	private long rcvLast = 0;
	private volatile long bpsOut = 0;
	private long sntLast = 0;

	private boolean running = true;

	public NodeMonitorImpl(String nodeId) {
		this.nodeId = nodeId;
	}
	
	public NodeMonitorImpl(String nodeId, int interval, String netinterface) {
		this.nodeId = nodeId;
		this.interval = interval;
		this.netinterface = netinterface;
	}

	@Override
	public double getCpuUsage() {
		return cpu;
	}

	@Override
	public double getMemoryUsage() {
		return memory;
	}

	@Override
	public int getNoCpuCores() {
		return noCores;
	}

	@Override
	public long getBpsIn(){
		return bpsIn;
	}
	
	@Override
	public long getBpsOut(){
		return bpsOut;
	}
	
	@Override
	public NodeMonitorInfo getNodeMonitorInfo() {
		return new NodeMonitorInfo(nodeId, noCores, cpu, memory, bpsIn, bpsOut);
	}
	
	public void start() {
		Thread t = new Thread(new MonitorThread());
		t.start();
	}

	public void stop() {
		running = false;
	}

	private class MonitorThread implements Runnable {
		public void run() {
			if (interval > 0) {
				while (running) {
					try {
						String line = null;

						// total cpu monitor
						File cpuFile = new File("/proc/stat");
						BufferedReader cpuReader = new BufferedReader(
								new FileReader(cpuFile));
						String cpuInfo = cpuReader.readLine();

						StringTokenizer t = new StringTokenizer(cpuInfo, " ");
						t.nextToken();
						long umode = Long.parseLong(t.nextToken());
						long nmode = Long.parseLong(t.nextToken());
						long smode = Long.parseLong(t.nextToken());
						long idle = Long.parseLong(t.nextToken());
						long work = umode + nmode + smode;

						cpuWork = work - cpuWorkLast;
						cpuIdle = idle - cpuIdleLast;

						cpu = 100 * ((double) (cpuWork)) / (cpuWork + cpuIdle);

						cpuWorkLast = work;
						cpuIdleLast = idle;

						// System.out.println("CPU usage " + cpu + " %");

						int i = 0;
						while (cpuReader.readLine().startsWith("cpu")) {
							i++;
						}
						// keep max no of seen active processors
						// due to hotplug this can change over time
						if (i > noCores)
							noCores = i;

						cpuReader.close();

						// process memory monitor
						File memFile = new File("/proc/meminfo");
						BufferedReader memReader = new BufferedReader(
								new FileReader(memFile));

						memory = 0;
						while ((line = memReader.readLine()) != null) {
							if (line.startsWith("MemTotal")) {
								StringTokenizer st = new StringTokenizer(line,
										" ");
								st.nextToken();
								memTotal = Long.parseLong(st.nextToken());
							} else if (line.startsWith("MemFree")) {
								StringTokenizer st = new StringTokenizer(line,
										" ");
								st.nextToken();
								memFree = Long.parseLong(st.nextToken());
							} else if (line.startsWith("Buffers")) {
								StringTokenizer st = new StringTokenizer(line,
										" ");
								st.nextToken();
								memBuffers = Long.parseLong(st.nextToken());
							} else if (line.startsWith("Cached")) {
								StringTokenizer st = new StringTokenizer(line,
										" ");
								st.nextToken();
								memCached = Long.parseLong(st.nextToken());
							}
						}

						memory = 100 * ((double) (memTotal - memFree - memBuffers - memCached))
								/ memTotal;

						// System.out.println("Memory usage is "+ memory +" %");
						memReader.close();

						// Bandwidth monitor
						File networkFile = new File("/proc/net/dev");
						BufferedReader networkReader = new BufferedReader( new FileReader(networkFile));
						line = null;

						long bytesRecieved = 0;
						long bytesSent = 0;
						while ((line = networkReader.readLine()) != null) {
							StringTokenizer st = new StringTokenizer(line, " ");
							String token = st.nextToken();
							if (token.contains(":") && (netinterface==null || token.contains(netinterface))) {
								String b = token
										.substring(token.indexOf(':') + 1);
								if (b.length() != 0)
									bytesRecieved += Long.parseLong(b);
								else
									bytesRecieved += Long.parseLong(st
											.nextToken());

								for (int k = 0; k < 7; k++)
									st.nextToken();

								bytesSent += Long.parseLong(st.nextToken());
							}

						}

						bpsIn = (bytesRecieved - rcvLast)*8/interval;
						rcvLast = bytesRecieved;

						bpsOut = (bytesSent - sntLast)*8/interval;
						sntLast = bytesSent;
						
						networkReader.close();
						
						//System.out.println("NODE MONITOR \t CPU\t"+cpu+"\t Memory \t"+memory+" BW in "+bpsIn+" BW out "+bpsOut);

						Thread.sleep(interval * 1000);
					} catch (IOException e) {
						running = false;
						Activator.logger.log(LogService.LOG_WARNING, "NodeMonitoring disabled: " + e.getLocalizedMessage());
					} catch (InterruptedException e) {
						Activator.logger.log(LogService.LOG_ERROR, "NodeMonitoring interupted", e);
					}
				}
			}

		}
	}

}
