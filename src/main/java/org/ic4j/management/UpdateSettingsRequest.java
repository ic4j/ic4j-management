/*
 * Copyright 2021 Exilor Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.ic4j.management;

import java.math.BigInteger;
import java.util.Optional;

import org.ic4j.candid.annotations.Field;
import org.ic4j.candid.annotations.Name;
import org.ic4j.candid.types.Type;
import org.ic4j.types.Principal;

public final class UpdateSettingsRequest {
    @Name("canister_id")
    @Field(Type.PRINCIPAL)
    public Principal canisterId;		
    @Name("settings")
    @Field(Type.RECORD)
    public CanisterSettings settings;
    @Name("sender_canister_version")
    @Field(Type.NAT)
    public Optional<BigInteger> senderCanisterVersion;    
}
