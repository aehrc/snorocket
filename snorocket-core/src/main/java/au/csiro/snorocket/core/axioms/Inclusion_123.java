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

package au.csiro.snorocket.core.axioms;

import au.csiro.snorocket.core.IFactory_123;

abstract public class Inclusion_123 {

    abstract public Inclusion_123[] normalise1(IFactory_123 factory);

    abstract public Inclusion_123[] normalise2(IFactory_123 factory);

    @Override
    abstract public int hashCode();

    @Override
    abstract public boolean equals(Object o);

    @Override
    abstract public String toString();

    abstract public NormalFormGCI getNormalForm();

}
