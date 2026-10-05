package org.rsmod.content.areas.misc.stronghold_of_security

internal class SecurityAnswer(val text: String, val response: String, val correct: Boolean)

internal class SecurityQuestion(val text: String, val answers: List<SecurityAnswer>) {
    init {
        require(answers.size in 2..3) { "Door questions offer two or three answers: $text" }
        require(answers.count { it.correct } == 1) { "Exactly one answer must pass: $text" }
    }

    val correctIndex: Int
        get() = answers.indexOfFirst { it.correct }
}

private fun right(text: String, response: String) = SecurityAnswer(text, response, correct = true)

private fun wrong(text: String, response: String) = SecurityAnswer(text, response, correct = false)

private fun question(text: String, vararg answers: SecurityAnswer) =
    SecurityQuestion(text, answers.toList())

internal object SecurityQuestions {
    const val PREAMBLE = "To pass you must answer me this:"

    val all: List<SecurityQuestion> =
        listOf(
            question(
                "What do you do if someone asks you for your password or bank PIN to make you a " +
                    "member for free?",
                wrong(
                    "Give them the information they asked for.",
                    "Wrong! Membership requires a subscription paid or a bond to be redeemed - " +
                        "they are trying to trick you. Never give your account details to anyone. " +
                        "Press the 'Report Abuse' button and fill in the offending player's name " +
                        "and the correct category.",
                ),
                wrong(
                    "Don't tell them anything and ignore them.",
                    "Quite good. But we should try to stop scammers. So please report them using " +
                        "the 'Report Abuse' button.",
                ),
                right(
                    "Don't tell them anything and click the 'Report Abuse' button.",
                    "Correct! Press the 'Report Abuse' Button and fill in the offending player's " +
                        "name and correct category.",
                ),
            ),
            question(
                "You have been offered a free giveaway or double XP invitation via in-game chat " +
                    "or email. What should you do?",
                right(
                    "Report the incident and do not click any links.",
                    "Correct! This is an attempt to obtain your account details. If it happened " +
                        "in the game, report it via the Report Abuse option.",
                ),
                wrong(
                    "Respond quickly so as not to miss the offer.",
                    "Wrong! Jagex will NEVER make offers like this - it is an attempt to obtain " +
                        "your account details. If it happens in the game, report via the Report " +
                        "Abuse option.",
                ),
            ),
            question(
                "You have been offered a free giveaway or double XP invitation via social media " +
                    "or a livestream. What should you do?",
                wrong(
                    "Respond quickly so as not to miss the offer.",
                    "Wrong! Jagex will NEVER make offers like this - it is an attempt to obtain " +
                        "your account details. Please report the incident using whatever " +
                        "reporting methods offered by the site you are on.",
                ),
                right(
                    "Report the incident and do not click any links.",
                    "Correct! This is an attempt to obtain your account details and should be " +
                        "reported to the relevant organisations via their own reporting systems.",
                ),
            ),
            question(
                "Is it safe to get someone to level your account?",
                wrong(
                    "Yes, so long as you change your password when they have finished.",
                    "Wrong! If you allow someone to use your account they may steal it. " +
                        "Alternatively they may use macros/bots to increase your level, and thus " +
                        "get the account banned.",
                ),
                wrong(
                    "Yes, so long as they don't raise its levels by too much.",
                    "Wrong! If you allow someone to use your account they may steal it. " +
                        "Alternatively they may use macros/bots to increase your level, and thus " +
                        "get the account banned.",
                ),
                right(
                    "No, you should never allow anyone to use your account.",
                    "Correct! Allowing someone else to use your account means they may steal it, " +
                        "or may get it banned by cheating on it.",
                ),
            ),
            question(
                "Hey adventurer! You've been randomly selected for a prize of 1 year of free " +
                    "membership! I'm just going to need some of your account details so I can " +
                    "put it on your account!",
                wrong(
                    "Wowee! Let me just write those down for you.",
                    "Incorrect! Do not trust anybody claiming you have won a prize and asking " +
                        "for your account details.",
                ),
                right(
                    "No way! I'm reporting you to Jagex!",
                    "Correct! Make sure to report anybody asking for your account login details.",
                ),
                wrong(
                    "I'm not sure about this... let me get back to you on it.",
                    "Incorrect! Well done for declining to offer but make sure to report players " +
                        "asking for your account details.",
                ),
            ),
            question(
                "What is the best security step you can take to keep your registered email secure?",
                right(
                    "Set up two-factor authentication with my email provider.",
                    "Correct! Enabling two-factor authentication on your email account will help " +
                        "protect your email from hijackers.",
                ),
                wrong(
                    "Have a complicated password set to my email.",
                    "Wrong! Complicated passwords are always a good idea but they are not the " +
                        "very best security step you can take to keep your email secure.",
                ),
                wrong(
                    "Use an email address just for Old School RuneScape.",
                    "Wrong! Even if you don't use your email address elsewhere there is still a " +
                        "risk that somebody could access it.",
                ),
            ),
            question(
                "What is the best way to secure your account?",
                wrong(
                    "A long and complicated password because you have good memory.",
                    "Incorrect! While a strong password is recommended, two-factor " +
                        "authentication adds extra levels of security.",
                ),
                right(
                    "Two-factor authentication on your account and your registered email.",
                    "Correct! Using each of these features will maximise your account security.",
                ),
            ),
            question(
                "How do I set a bank PIN?",
                right(
                    "Talk to any banker.",
                    "Correct! Simply talking to a banker will give you the option to set a bank " +
                        "PIN. Never use personal details for passwords or bank PINs!",
                ),
                wrong(
                    "Use the account management section on the website.",
                    "Wrong! Your password can be changed from the account management section, " +
                        "but you must talk to a banker to set a bank PIN. Never use personal " +
                        "details for passwords or bank PINs!",
                ),
            ),
            question(
                "What should I do if I receive an email asking me to verify my identity or " +
                    "account details due to suspicious activity?",
                wrong(
                    "Email them back with the information it asks for.",
                    "Wrong! Jagex will NEVER email to ask for account details, so don't send " +
                        "any info back.",
                ),
                wrong(
                    "Click the links in the email to visit the website.",
                    "Wrong! Jagex will NEVER email to ask for account details, and the links may " +
                        "lead to a fake website.",
                ),
                right(
                    "Delete it - it is fake!",
                    "Correct! Jagex will NEVER email you unless you've used the website to " +
                        "change your account. Account details can only be verified through using " +
                        "account recovery systems on the Old School RuneScape website, NOT " +
                        "through responding to emails.",
                ),
            ),
            question(
                "Who can I give my password to?",
                wrong(
                    "My friends.",
                    "Wrong! Your password should be kept secret from everyone. You should " +
                        "*never* give it out under any circumstances.",
                ),
                wrong(
                    "My brother or sister.",
                    "Wrong! Your password should be kept secret from everyone. You should " +
                        "*never* give it out under any circumstances.",
                ),
                right(
                    "Nobody.",
                    "Correct! Your password should be kept secret from everyone. You should " +
                        "*never* give it out under any circumstances.",
                ),
            ),
            question(
                "What do I do if my account is compromised?",
                wrong(
                    "Post on social media about it.",
                    "Wrong! Social media will often not solve the problem directly. You'd " +
                        "eventually need to go to the Old School RuneScape website to regain " +
                        "access to your account.",
                ),
                right(
                    "Secure my device and reset my password.",
                    "Correct! Securing your systems with virus and malware scans is important " +
                        "before resetting your account password to ensure your details are not " +
                        "compromised again. Don't forget to secure your email account too!",
                ),
            ),
            question(
                "What do I do if a moderator asks me for my account details?",
                wrong(
                    "Tell them whatever they want to know.",
                    "Wrong! Never give your account details to anyone! This includes things like " +
                        "account creation details, contact details and passwords. Never use " +
                        "personal details for passwords or bank PINs!",
                ),
                wrong(
                    "Politely tell them no, and ignore them.",
                    "Okay! Don't just tell them the details. But reporting the incident to " +
                        "Jagex would help. Use the Report Abuse button. Never use personal " +
                        "details for passwords or bank PINs!",
                ),
                right(
                    "Politely tell them no, then use the 'Report Abuse' button.",
                    "Correct! Report any attempt to gain your account details as it is a very " +
                        "serious breach of Old School RuneScape's rules. Never use personal " +
                        "details for passwords or bank PINs!",
                ),
            ),
            question(
                "A player trades you some valuable items, provides you with a bond, then asks " +
                    "if you want to share your account so he can help you make progress. How do " +
                    "you respond?",
                right(
                    "Decline the offer and report that player.",
                    "Correct! Never share your login details with another player.",
                ),
                wrong(
                    "Give the player access since they are a higher level.",
                    "Incorrect! Never share your login details with another player.",
                ),
                wrong(
                    "Tell them you'll trade your account info for theirs.",
                    "Incorrect! This option still puts your account and items at risk!",
                ),
            ),
            question(
                "Where is it safe to use my Old School RuneScape password?",
                wrong(
                    "On Old School RuneScape and all fansites.",
                    "Wrong! Always use a unique password purely for your Old School RuneScape " +
                        "account.",
                ),
                right(
                    "Only on the Old School RuneScape website.",
                    "Correct! Always make sure you are entering your password only on the Old " +
                        "School RuneScape website as other sites may try to steal it.",
                ),
                wrong(
                    "On all websites I visit.",
                    "Wrong! This is very insecure and may lead to your account being stolen.",
                ),
            ),
            question(
                "Whose responsibility is it to keep your account secure?",
                right(
                    "Me.",
                    "Correct! Make sure to use the tools Jagex recommend, such as two-factor " +
                        "authentication options.",
                ),
                wrong(
                    "Jagex.",
                    "Incorrect! Jagex can offer tools such as two-factor authentication " +
                        "options, but you must make sure you use them correctly to keep your " +
                        "info safe!",
                ),
                wrong(
                    "My internet provider.",
                    "Incorrect! Your internet service provider may offer security advice, but " +
                        "use the tips on the Old School RuneScape website to stay secure.",
                ),
            ),
            question(
                "Psst! Adventurer! I've got a special offer for you, but you're going to have " +
                    "to trust me. If you give me some gold coins, I'll give you back twice " +
                    "whatever you gave me! How does that sound?",
                right(
                    "No way! You'll just take my gold for your own! Reported!",
                    "Correct! If it sounds too good to be true, it probably is! Be wary of " +
                        "these types of scams.",
                ),
                wrong(
                    "I'm not sure... but giving a few coins to test it won't hurt.",
                    "Incorrect! Do not trust players asking for gold offering to return a " +
                        "higher amount.",
                ),
                wrong(
                    "WoW! You're so generous, thank you! Here's all my gold.",
                    "Incorrect! Do not trust players asking for gold offering to return a " +
                        "higher amount.",
                ),
            ),
            question(
                "Is it okay to buy an Old School RuneScape account?",
                wrong(
                    "Yes if it is from someone you know.",
                    "Wrong! If you buy an account, the person who originally made it may take " +
                        "it back, and you will lose anything you paid for it.",
                ),
                wrong(
                    "Yes if you pay for it with GP.",
                    "Wrong! If you buy an account, the person who originally made it may take " +
                        "it back, and you will lose anything you paid for it.",
                ),
                right(
                    "No, you should never buy an account.",
                    "Correct! Buying accounts is against the rules. Also you could lose the " +
                        "account if the original owner takes it back.",
                ),
            ),
            question(
                "My friend asks me for my password so that he can do a difficult quest for me. " +
                    "Do I give it to him?",
                wrong(
                    "Yes. He is my best friend and I've already spent ages trying this quest.",
                    "Wrong! Don't give your password to anyone otherwise you can lose " +
                        "everything you have worked so hard for.",
                ),
                right(
                    "Don't give them my password.",
                    "Correct! You can make it alone and the success will taste even better. " +
                        "Don't forget you can ask people for advice too!",
                ),
                wrong(
                    "Let them do the quest, but in the same room the whole time.",
                    "Wrong! Never let anyone use your account for any reason - they might try " +
                        "to keep it by changing the password! You'd be held responsible if they " +
                        "broke the rules on your account too.",
                ),
            ),
            question(
                "A player tells you to search for a video online, click the link in the " +
                    "description and comment on the forum post to win a cash prize. What do you " +
                    "do?",
                wrong(
                    "Do what they ask, using the provided link in the video description.",
                    "Incorrect! Don't trust these types of link even if they look similar to " +
                        "the Old School RuneScape website.",
                ),
                right(
                    "Report the player for phishing.",
                    "Correct! Always be wary of these links and double check you are on the " +
                        "official Old School RuneScape website before entering your login " +
                        "details!",
                ),
                wrong(
                    "Tell your friends so they can get free gold too.",
                    "Incorrect! Don't trust these types of link even if they look similar to " +
                        "the Old School RuneScape website.",
                ),
            ),
            question(
                "Adventurer, I'll trade items with you for an amazing price, but you've got to " +
                    "come immediately to a particular place on a different game world. Hurry " +
                    "up! Come now before you lose out! What do you say?",
                wrong(
                    "Okay, I'll take my valuables there now so we can trade.",
                    "Incorrect! Do not trust players trying to rush you into going somewhere " +
                        "unfamiliar. It's often an attempt to lure you into taking items " +
                        "somewhere dangerous, where you'll lose them.",
                ),
                right(
                    "Nope, you're tricking me into going somewhere dangerous.",
                    "Correct! If it sounds too good to be true, it probably is! Be wary of " +
                        "these types of scams.",
                ),
            ),
            question(
                "Which of these is an important characteristic of a secure password?",
                wrong(
                    "It incorporates your real name or birthday.",
                    "Incorrect! Using personal details as your password makes it easier to guess!",
                ),
                right(
                    "It's never used on other websites or accounts.",
                    "Correct! Make sure to use a unique password to keep your account secure.",
                ),
                wrong(
                    "It's never changed over many months or years.",
                    "Incorrect! You should change your password frequently.",
                ),
            ),
            question(
                "You're watching a stream by someone claiming to be Jagex offering double XP. " +
                    "What do you do?",
                wrong(
                    "Click the link! I love double XP!",
                    "Incorrect! This is a common phishing method and puts your account at risk!",
                ),
                right(
                    "Report the stream. Real Jagex streams have a 'verified' mark.",
                    "Correct! This is a common phishing method and puts your account at risk!",
                ),
                wrong(
                    "Ignore it.",
                    "Incorrect! Well done for avoiding the phishing attempt but make sure to " +
                        "report these wherever possible to help other players.",
                ),
            ),
            question(
                "Will Jagex prevent me from saying my PIN in game?",
                wrong(
                    "Yes.",
                    "Wrong! Jagex does NOT block your PIN so don't type it! Anyone asking you to " +
                        "say your PIN is trying to trick you.",
                ),
                right(
                    "No.",
                    "Correct! Jagex will not block your PIN so don't type it! Anyone asking you " +
                        "to say your PIN is trying to trick you.",
                ),
            ),
            question(
                "A website claims that they can make me a player moderator. What should I do?",
                right(
                    "Nothing, it's a fake.",
                    "Correct! Remember that moderators are hand picked by Jagex and contact is " +
                        "made through the game inbox only.",
                ),
                wrong(
                    "Give them my account info and password.",
                    "Wrong! This will almost certainly lead to your account being hijacked. No " +
                        "website can make you a moderator as they are hand picked by Jagex.",
                ),
            ),
            question(
                "What do I do if I think I have a keylogger or virus?",
                right(
                    "Virus scan my device then change my password.",
                    "Correct! Removing the keylogger must be the priority, otherwise anything " +
                        "you type can be given away. Remember to change your password and bank " +
                        "PIN afterwards.",
                ),
                wrong(
                    "Change my password then virus scan my device.",
                    "Wrong! If you change your password while you still have the keylogger, it " +
                        "will still be insecure. Remove the keylogger first. Never use personal " +
                        "details for passwords or bank PINs!",
                ),
                wrong(
                    "Nothing, it will go away on its own.",
                    "Wrong! This could mean your account may be accessed by someone else. " +
                        "Remove the keylogger then change your password. Never use personal " +
                        "details for passwords or bank PINs!",
                ),
            ),
            question(
                "What should you do if another player messages you recommending a website to " +
                    "purchase items and/or gold?",
                wrong(
                    "Check out the website, it never hurts to look around!",
                    "Incorrect! Websites offering these services should not be trusted!",
                ),
                wrong(
                    "Visit the website in a private browser for added security.",
                    "Incorrect! Websites offering these services should not be trusted!",
                ),
                right(
                    "Do not visit the website and report the player who messaged you.",
                    "Correct! Buying and selling items and gold is against the rules and " +
                        "results in a permanent ban!",
                ),
            ),
            question(
                "Can I leave my account logged in while I'm out of the room?",
                wrong(
                    "Yes, for up to an hour.",
                    "Wrong! You should log out in case you are attacked. Leaving your character " +
                        "logged in can also allow someone to steal your items or your entire " +
                        "account!",
                ),
                right(
                    "No.",
                    "Correct! This is the safest, both in terms of security and keeping your " +
                        "items! Leaving your character logged in can also allow someone to " +
                        "steal your items or your entire account!",
                ),
                wrong(
                    "Yes, if I'll only be a minute or two.",
                    "Wrong! You should log out in case you are attacked. Leaving your character " +
                        "logged in can also allow someone to steal your items or your entire " +
                        "account!",
                ),
            ),
            question(
                "What do you do if someone asks you for your password or bank PIN to make you a " +
                    "player moderator?",
                right(
                    "Don't give them the information and send an 'Abuse report'",
                    "Correct! Press the 'Report Abuse' button and fill in the offending " +
                        "player's name and the correct category.",
                ),
                wrong(
                    "Don't tell them anything and ignore them.",
                    "Quite good. But we should try to stop scammers. So please report them using " +
                        "the 'Report Abuse' button.",
                ),
                wrong(
                    "Give them the information they asked for.",
                    "Wrong! Jagex never ask for your account information - especially to " +
                        "become a player moderator. Press the 'Report Abuse' button and fill in " +
                        "the offending player's name and the correct category.",
                ),
            ),
            question(
                "What is an example of a good bank PIN?",
                wrong(
                    "Your real life bank PIN.",
                    "This is a bad idea as if someone happens to find out your bank PIN on Old " +
                        "School RuneScape, they then have your real life bank PIN! Never use " +
                        "personal details for passwords or bank PINs!",
                ),
                wrong(
                    "Your birthday.",
                    "Not a good idea. You know how many presents you get for your birthday, so " +
                        "you can imagine how many people know this date. Never use personal " +
                        "details for passwords or bank PINs.",
                ),
                right(
                    "The birthday of a famous person or event.",
                    "Well done! Unless you tell someone, they are unlikely to guess who or what " +
                        "you have chosen, and you can always look it up. Never use personal " +
                        "details for passwords or bank PINs!",
                ),
            ),
            question(
                "What should you do if your real-life friend asks for your password so he can " +
                    "check your stats?",
                wrong(
                    "Give them your password since they're a friend in real life.",
                    "Incorrect! Don't trust anybody with your account login details!",
                ),
                right(
                    "Don't give out your password to anyone. Not even close friends.",
                    "Correct! Doing so could result in losing your items and gold and puts your " +
                        "account at risk.",
                ),
                wrong(
                    "Log in for your friend and let them play.",
                    "Incorrect! Never allow anybody access to your account.",
                ),
            ),
            question(
                "A player starts asking you about very specific details linked to your " +
                    "account, such as when you created your account, your birthday date, " +
                    "internet provider etc. How should you react?",
                wrong(
                    "Be friendly and answer the questions.",
                    "Incorrect! These details can be used within the account recovery system " +
                        "and possibly compromise your account.",
                ),
                right(
                    "Don't share your information and report the player.",
                    "Correct! These details could be used within the account recovery system " +
                        "and possibly compromise your account.",
                ),
                wrong(
                    "Answer questions and ask the player back for their details.",
                    "Incorrect! These details can be used within the account recovery system " +
                        "and possibly compromise your account. If you're asking the player back, " +
                        "you're unknowingly committing the same offence.",
                ),
            ),
            question(
                "How do I remove a hijacker from my account?",
                wrong(
                    "Ask on social media.",
                    "Wrong! Visiting oldschool.runescape.com and using the account recovery " +
                        "system will allow you to kick hijackers off your account by locking it " +
                        "as stolen.",
                ),
                wrong(
                    "Email Old School RuneScape.",
                    "Wrong! Visiting oldschool.runescape.com and using the account recovery " +
                        "system will allow you to kick hijackers off your account by locking it " +
                        "as stolen.",
                ),
                right(
                    "Use the Account Recovery system.",
                    "Correct! Visiting oldschool.runescape.com and using the account recovery " +
                        "system will allow you to kick hijackers off your account by locking it " +
                        "as stolen.",
                ),
            ),
            question(
                "You are part way through the Stronghold of Security when you have to answer " +
                    "another question. After you answer the question, you should...",
                wrong(
                    "Click through the text as fast as possible to get your reward sooner.",
                    "Incorrect! You should read the text and follow the advice to keep your " +
                        "account as secure as possible.",
                ),
                wrong(
                    "Read the text, but forget the info moments later.",
                    "Incorrect! You should always remember this info so your account is as safe " +
                        "as possible.",
                ),
                right(
                    "Read the text and follow the advice given.",
                    "Correct! As tempting as it is to click through all the text and get your " +
                        "reward as soon as possible, it's important to know these security tips.",
                ),
            ),
        )
}
