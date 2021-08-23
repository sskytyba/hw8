Indexes hw8:  
table size:
    > 40_000_000  
table:
- id - integer
- date - datetime
<table>
    <thead>
        <th>Filter</th>
<th>With index</th>
<th>Without index</th>
    </thead>
    <tbody>
<tr>
<td>
    BETWEEN '1990-07-01 00:00:00' AND '1990-07-20 00:00:00'
    </td>
<td>
1.457 s
    </td>
<td>
54 s
    </td>
</tr>
<tr>
<td>
    = '1979-07-31 20:13:20'
    </td>
<td>
33 ms
    </td>
<td>
12 s
    </td>
</tr>
<tr>
<td>
    > 2001-04-01 00:00:00
    </td>
<td>
9.37 s (600_000 rows)
    </td>
<td>
13s (600_000 rows)
    </td>
</tr>
<tr>
<td>
    like '2001-04-__ __:__:__'
    </td>
<td>
13 s (115_000 rows)
    </td>
<td>
50 s (115_000 rows)
    </td>
</tr>
<tr>
<td>
    like '____-__-__ 04:40:00'
    </td>
<td>
12 s (115_000 rows)
    </td>
<td>
40 s (115_000 rows)
    </td>
</tr>
<tr>
<td>
    count(*) + rand_datetime where rand_datetime = '1979-07-31 20:13:20'
    </td>
<td>
3 ms
    </td>
<td>
12 s
    </td>
</tr>
<tr>
<td>
    count(id) + rand_datetime where rand_datetime = '1979-07-31 20:13:20'
    </td>
<td>
3 ms
    </td>
<td>
12 s
    </td>
</tr>

</tbody>
</table>

Transaction isolation hw 8.1:<br/><br/>

Phantom read: Isolation: SERIALIZABLE, value [10, 15, 20] vs. [10, 15, 20]<br/>
Phantom read: Isolation: REPEATABLE_READ, value [10, 15, 20] vs. [10, 15, 20, 26]<br/>
Phantom read: Isolation: READ_COMMITTED, value [10, 15, 20] vs. [10, 15, 20, 25]<br/>
Phantom read: Isolation: READ_UNCOMMITTED, value [10, 15, 20] vs. [10, 15, 20, 25]<br/>
<br/>
Lost update: Isolation: SERIALIZABLE, value 30 vs. 30 <br/>
Lost update: Isolation: REPEATABLE_READ, value 30 vs. 30 <br/>
Lost update: Isolation: READ_COMMITTED, value 30 vs. 20 <br/>
Lost update: Isolation: READ_UNCOMMITTED, value 30 vs. 20 <br/>
<br/>
Dirty read: Isolation: SERIALIZABLE, value 10 vs. 10 <br/>
Dirty read: Isolation: REPEATABLE_READ, value 10 vs. 10<br/>
Dirty read: Isolation: READ_COMMITTED, value 10 vs. 10<br/>
Dirty read: Isolation: READ_UNCOMMITTED, value 10 vs. 25<br/>
<br/>
Unrepeatable read: Isolation: SERIALIZABLE, value 10 vs. 10<br/>
Unrepeatable read: Isolation: REPEATABLE_READ, value 10 vs. 10<br/>
Unrepeatable read: Isolation: READ_COMMITTED, value 10 vs. 25<br/>
Unrepeatable read: Isolation: READ_UNCOMMITTED, value 10 vs. 25<br/>
<br/>