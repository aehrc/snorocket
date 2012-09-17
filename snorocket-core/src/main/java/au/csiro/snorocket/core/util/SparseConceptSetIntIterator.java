/**
 * Copyright (c) 2009 International Health Terminology Standards Development
 * Organisation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */

package au.csiro.snorocket.core.util;

/**
 * {@link IntIterator} used in the {@link SparseConceptSet} implementation.
 * 
 * @author Alejandro Metke
 *
 */
public class SparseConceptSetIntIterator implements IntIterator {
	
	private int[] items;
    private int size;
    private int next = 0;
	
	public SparseConceptSetIntIterator(int[] i, int size) {
		synchronized (this) {
			items = new int[i.length];
			System.arraycopy(i, 0, items, 0, items.length);
			this.size = size;
		}
	}
	
	@Override
	public boolean hasNext() {
		return next < size;
	}

	@Override
	public int next() {
		return hasNext() ? items[next++] : -1;
	}

}
