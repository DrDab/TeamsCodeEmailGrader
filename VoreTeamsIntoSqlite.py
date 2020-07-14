#!/usr/bin/python3
import csv
import sqlite3

CSV_FILENAME = "signups.csv"
SQLITE_FILENAME = "graderdata.sqlite"

conn = sqlite3.connect(SQLITE_FILENAME)
c = conn.cursor()
c.execute("CREATE TABLE IF NOT EXISTS contestantsDb(name TEXT, division TEXT, email1 TEXT NULL, email2 TEXT NULL, email3 TEXT NULL, email4 TEXT NULL, email5 TEXT NULL, email6 TEXT NULL)")
conn.commit()

print("Adding teams...")

with open(CSV_FILENAME, newline='') as csvfile:
    reader = csv.DictReader(csvfile)
    for row in reader:
        teamName = row['Team Name']
        division = row['Which division are you joining?'].upper()
        email1 = row['Team Member #1 Email']
        email2 = row['Team Member #2 Email']
        email3 = row['Team Member #3 Email']
        email4 = row['Team Member #4 Email']
        print ("%s [%s]) %s,%s,%s,%s" % (teamName, division, email1, email2, email3, email4))
        if email1.strip() == "":
            email1 = None
        if email2.strip() == "":
            email2 = None
        if email3.strip() == "":
            email3 = None
        if email4.strip() == "":
            email4 = None
        c.executemany("INSERT INTO contestantsDb(name, division, email1, email2, email3, email4) VALUES(?,?,?,?,?,?)", [(teamName, division, email1, email2, email3, email4)])
        conn.commit()
print("Done.")
conn.close()
        