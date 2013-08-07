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
package be.iminds.aiolos.rsa.util;

import java.lang.reflect.Method;

/**
 * Utility class for creating a method signature String from Method object
 */
public class MethodSignature {

	public static final int VOID = 0;
	public static final int BOOLEAN = 1;
	public static final int CHAR = 2;
	public static final int BYTE = 3;
	public static final int SHORT = 4;
	public static final int INT = 5;
	public static final int FLOAT = 6;
	public static final int LONG = 7;
	public static final int DOUBLE = 8;
	public static final int ARRAY = 9;
	public static final int OBJECT = 10;

	public static String getMethodSignature(final Method m) {
		Class<?>[] parameters = m.getParameterTypes();
		StringBuffer buf = new StringBuffer();
		buf.append(m.getName());
		buf.append('(');
		for (int i = 0; i < parameters.length; ++i) {
			getDescriptor(buf, parameters[i]);
		}
		buf.append(')');
		getDescriptor(buf, m.getReturnType());
		return buf.toString();
	}

	private static void getDescriptor(final StringBuffer buf, final Class<?> c) {
		Class<?> d = c;
		while (true) {
			if (d.isPrimitive()) {
				char car;
				if (d == Integer.TYPE) {
					car = 'I';
				} else if (d == Void.TYPE) {
					car = 'V';
				} else if (d == Boolean.TYPE) {
					car = 'Z';
				} else if (d == Byte.TYPE) {
					car = 'B';
				} else if (d == Character.TYPE) {
					car = 'C';
				} else if (d == Short.TYPE) {
					car = 'S';
				} else if (d == Double.TYPE) {
					car = 'D';
				} else if (d == Float.TYPE) {
					car = 'F';
				} else /* if (d == Long.TYPE) */{
					car = 'J';
				}
				buf.append(car);
				return;
			} else if (d.isArray()) {
				buf.append('[');
				d = d.getComponentType();
			} else {
				buf.append('L');
				String name = d.getName();
				int len = name.length();
				for (int i = 0; i < len; ++i) {
					char car = name.charAt(i);
					buf.append(car == '.' ? '/' : car);
				}
				buf.append(';');
				return;
			}
		}
	}

}
