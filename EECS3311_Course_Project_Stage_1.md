## 1.1: Project Description 

AI Personal Finance Assistant 

## What problem does your project solve? 

The AI Personal Finance Assistant helps users keep track of their finances. It allows them to keep track of their earnings and spending and break it down into categories, allowing users to see what is necessary and unnecessary spending. Through this, the personal AI helps the user by making suggestions on what can be changed in their spending if prompted, and allows the user to save towards a goal. 

## Who are the target users? 

The target users are individuals who want to better understand and manage their personal finances. This includes people who want to monitor their expenses, manage monthly budgets, work toward savings goals, or identify areas where they can reduce spending. Users manually enter their financial information, allowing them to track their finances without requiring direct access to their bank accounts. 

## What can the agent do? 

The AI Personal Finance Assistant will provide the following capabilities: 

- Answer users' financial questions through an AI chatbox. 

- Monthly expense tracking 

- Generate personalized monthly or weekly budget recommendations based on the user's income and spending habits. 

- Recommend ways to improve savings based on the user's savings goals and expenses. 

- Compare planned budgets with actual spending to help users identify overspending. 

- Provide explanations for its recommendations so users can understand the reasoning behind them. 

Why is an AI agent appropriate for this project? 

An AI agent is appropriate because personal finance management involves more than displaying balances or performing calculations. Users may need help interpreting spending patterns, identifying unnecessary expenses, and deciding how to allocate their money. The agent can analyze the financial information provided by the user, use it as context when answering questions and generate recommendations for the user’s situation. 

## Which AI model do you plan to use? 

The project plans to use ChatGPT 5 as its AI. The model will support the financial chatbox, interpret users' questions, analyze spending information, and generate personalized budgeting and savings recommendations. 

How will the AI model interact with the rest of the system? 

The AI model will interact with the application's graphical user interface (GUI), command-line interface (CLI), backend logic, and database through the application's AI integration component. When the user requests advice, the backend will provide the relevant information by retrieving it from the database and giving the user's question to the AI model. The model will generate a response or recommendation, which the application will return to the user through the appropriate interface. The application will remain 

responsible for validating user input, retrieving and storing financial records, performing calculations, and checking budget totals. The AI model will focus on interpreting information and generating explanations and recommendations. If the information required for a recommendation is missing, the assistant will ask the user to provide it as opposed to making unsupported assumptions. 

## Graphical User Interface (GUI) 

The GUI will provide a visual and user-friendly way to interact with the assistant. Users will be able to view their financial dashboard, manage balances and spending categories, record and review expenses, create savings goals, compare budgets against actual spending, and interact with the AI chatbox. The interface will display financial information and AI-generated recommendations in an understandable format so users can review their financial situation and make informed decisions. 

## Command-Line Interface (CLI) 

The CLI will provide a text-based way to interact with the assistant. Users will be able to enter commands or responses to manage financial information and access the system's main features, including viewing balances and categories, managing expenses and savings goals, requesting budget and savings recommendations, comparing planned budgets with actual spending, and asking financial questions through the AI functionality. The CLI will validate user input and display results and error messages in text form. Both the GUI and CLI will use the same underlying application logic and financial data so that the system's core behaviour remains consistent across the two interfaces. 

## Agent Behavior 

The AI features are built as agents, not as single calls to the AI model. Each AI feature is handled by an agent class that extends the abstract class FinanceAgent: ExpenseCategorizerAI (F04), ChatAI (F06), BudgetAI (F07) and SavingsAI (F08). When a request arrives, the agent works through several steps: 

- Recall: reads earlier messages and the user's stated preferences from AgentMemory. 

- Prepare: buildPrompt() combines the request, the memory and the instructions for that feature. 

- Reason: sends the prompt to ChatGPT 5 through the AIService interface. 

- Use tools: if the model needs more information, it asks for a tool (GetFinancialDataTool, CompareBudgetTool or ProjectSavingsTool). ToolRegistry finds and runs the tool. The tools read data only through FinancialDataFacade, so the model never accesses the database directly. 

- Observe and repeat: the tool result is stored in memory and sent back to the model. The loop ends when the model gives a final answer or a maximum number of steps (maxSteps) is reached. 

- Explain: parseResult() turns the final answer into a Recommendation, a Budget, a suggested Category or a chat reply, with the reasoning written in plain language. 

This gives the agent reasoning, tool use, memory and multi-step task execution. The agent does not do the arithmetic itself. The application calculates totals and progress (calculateBaseline() in BudgetAI, calculateMonthlyTarget() in SavingsAI, calculateProgress() in SavingsGoal, calculateActual() and calculateDifference() in BudgetController), and the AI interprets and explains the results. If the information needed for a recommendation is missing, the agent asks the user for it instead of guessing. 

Classes that support the agents 

|Class|Role|Features|
|---|---|---|
|FinanceAgent|Abstract agent. run() holds the loop; buildPrompt()<br>and parseResult() are filled in by each subclass.<br>Holds an AIService, an AgentMemory, a<br>ToolRegistry and maxSteps.|F04, F06, F07, F08|
|ExpenseCategorizerA<br>I, ChatAI, BudgetAI,<br>SavingsAI|Agent subclasses: categorizeExpense() (F04),<br>processQuestion() (F06), analyzeFinances() with<br>calculateBaseline() (F07), analyzeSavingsGoal()<br>with calculateMonthlyTarget() (F08)|F04, F06, F07, F08|
|AgentMemory|add(), recall(): keeps messages and preferences<br>between steps|F04, F06, F07, F08|
|ToolRegistry|register(), execute(): finds and runs a tool by name|F06, F07, F08|
|AgentTool|Interface: getName(), execute(args). Implemented<br>by GetFinancialDataTool, CompareBudgetTool<br>and ProjectSavingsTool|F06, F07, F08|
|AIService|Interface: generateResponse(). Implemented by<br>AIServiceProxy and ChatGPTAdapter|F04, F06, F07, F08|
|Recommendation,<br>FinancialData|Data objects: an AI recommendation, and the<br>financial information passed to the agent|F07, F08|



## 1.2 Feature Specification: 

## F01 - Account Creation 

This feature will allow the user to create an account for the application. This will store the user’s login info, along with their financial information, savings goals, expenses, and any other data associated with their account. The user will select the create account option on the application’s login screen, and a form will appear where they can input information. The user will require user’s information, like an email and a phone number, along with confirming their password and whether they want to be reached by phone or email. It will tell the user an account has been created. This feature is deterministic. The user will select the create account option and enter required information. The system will check if the phone number or email has been used already. If everything is valid, it will create an account and save the information to a database. If a field is empty or information is invalid, the application will ask the user to fix the information. If a phone number or email is already associated with an account, it will ask the user to log into that account. 

## F02 - Dashboard receiving information. 

This will require the user to give the application their balance in their account. It will then display said information in a way that the user chooses. The input needed will be the user's inputted balance. The output will contain balance data and transaction data. The feature itself will have no AI involvement, so it will be deterministic. The expected workflow is that it will receive information that the user provides and display different accounts with different names depending on the user(s)’ choice. If invalid information is entered, like a non-number, it will ask the user to input information again. If invalid info is added, the system will notify the user with a message and ask them to try again. 

## F03 - Categories for dashboard 

These will be the categories containing the balance for the dashboard to display. The user will have to add a category (savings, checking, total amount, etc.) through an add category button in the middle of the GUI. After this is done, the user will have to add the balance they have into the category manually and update it. The feature requires the user’s input balance and what categories they want. It will output said balance into user-selected categories. The feature is deterministic. When the feature is executed, it should create a new category if the user selects this option and then display it. An expected error is if the user enters a non-integer. If this is the case, it will ask the user to input valid information. A negative number is okay, as the user’s account balance could be negative. 

## F04 - Monthly Expense 

This will assign a category to the chosen predetermined expense option (grocery, car payment, bills, etc.) and display what it costs for that month after it has been paid off. There will be a dropdown list displaying different categories, along with an option for a category created by the user to create the category. To use it, the user will go into Monthly expense and input options from there. The AI will confirm with the user if this is the correct category, and if it isn’t, it will either attempt again or allow the user to input it manually. The feature requires the user’s input on their expense. It produces an updated category field that is saved to the database. The AI will be hybrid. It will fetch new transactions and run them through the AI. If this does not work, the user will input the result manually. If the AI cannot match it to a category, it will notify the user, and it will not be added to the category until the user inputs it manually. 

## F05 - Save goal tracker 

This will track the user’s savings account to a goal that they select. The user will create a goal that they want to save towards through an option on the dashboard that will say goal tracker with an addition sign next to it. The feature requires a name (can be preselected), an amount the user can save toward, and a timeframe. The output will be a percentage and a progress bar displaying the amount the user has saved and how much is left. AI involvement will be deterministic. The expected workflow will be the user creating a goal, a time, and linking an account to it. The goal will update with the account’s balance. If the account is in the negative or the balance goes down, the progress bar and percentage will also reduce or display a "no money saved yet” message. 

## F06 - AI chatbox 

This will provide an interface where the user can communicate with an AI chatbox to ask questions. The user will type a question into the chatbox and will access it through the AI chatbox feature displayed on the dashboard. It will receive a string input from the user, which will then be given to an AI, and the AI’s response will be output. This feature is AI-based. The expected workflow is that the user will select the chatbox feature and input a string, which will then be given to an AI, and the chat will display the response from the AI. An error is if the AI is not working or is taking a long time to respond; it will prompt the user to ask again in a few minutes. 

## F07 - AI-powered suggested monthly and/or weekly budget 

This will use the user’s financial information to suggest a monthly or weekly budget in order to meet their savings goal in their timeframe. The AI will analyze the user’s spending habits and provide suggested spending for different categories, recognizing that some expenses cannot be avoided. The user will have a budget option on the dashboard and will select whether they want a monthly or weekly budget recommendation. The feature requires the user’s income, expenses, and spending categories. The output will have a suggested budget for the user, along with recommended amounts for the user’s categories. It will be AI-Based. The user will select the budget recommendation option and choose a monthly or weekly budget. The system will gather the user’s financial information and provide it to the AI, which will then analyze and generate a suggested budget that will be displayed to the user. If there isn't enough financial information to create a recommendation, the system will display a message asking for more information and allowing the user to try again in a day. 

## F08 - Make recommendations based on savings goal (EX: save this much) 

The feature will make recommendations on how much money they should save toward their savings goal. The user will access the feature through the savings goal on their dashboard and will be able to request a recommendation based on how much they should save. The feature will require the user’s savings goal, target amount, current amount saved, and income and expense information. It will output a recommended amount that the user should save on a weekly/monthly budget and make recommendations on how the user can reach their goal. It will be AI- based. The user will create or select a savings goal and request a savings recommendation. Their financial information will then be provided to an AI, which will analyze the information and recommend what should be saved, which will be displayed to the user. If there is not enough information, it will ask the user to provide the information and try again only after information is provided. 

## F09 - Budget vs. actual spent 

This feature will display the user’s budget next to the amount they spent in a given time period, along with the percentage that the user is under or over budget by. The user will access this through the budget section of the dashboard and select a time period. This feature requires the user’s budget information and recorded expense information. It will display the user’s budget, their actual spending, and the difference between the two (as percentages), along with individual spending categories to see where the difference comes from. AI will not be involved, as the system will be able to calculate the difference itself, so it will be deterministic. The application will retrieve the user’s budget and recorded expenses for the selected time period and calculate the amount the user has spent and compare it to their budget. This will then be displayed back to the user. If a budget has not been created, the system will notify the user that a budget is needed. If there is no recorded expense, the system will say that there are no expenses made. 

## F10 - 2FA 

This will provide a layer of security for the user by requiring the user to provide a verification code sent to their email or phone number. The user will interact with their phone or email address, where they will then input the 6-digit code into the application. The feature requires the user’s login information and a valid verification code. This feature will allow the user to access their account if the verification code is correct or deny access if the code is not. The feature will be deterministic. The expected workflow is that 

the user will enter a username and password. Provided this is correct, a code will be sent to the email or phone number, and the user will enter this into the GUI. The system verifies the code to make sure it's correct and allows access depending on the result. If an incorrect code is entered, a message will be sent to the user asking them to try again, up to 3 attempts. If the code expires, it will have an option for the user to request a new code. The user will be timed out if they input information incorrectly 3 times. 

## Task 2: Design System using UML 

## Task 2.1: Class Diagram 

The class diagram on the next page shows the major classes and interfaces of the system with their important attributes and methods, using the course notation: association, inheritance, realization, dependency, aggregation and composition, with multiplicities. The classes are arranged in layers from top to bottom: 

- GUI classes (LoginGUI, DashboardGUI, ExpenseGUI, SavingsGoalGUI, BudgetGUI, ChatGUI) collect input and display results. 

- Controllers (AccountController, DashboardController, ExpenseController, SavingsController, BudgetController, AIController) coordinate each request. 

- Services, patterns and AI components (AuthenticationService, FinancialDataFacade, SavingsGoalBuilder, TwoFactorService and its proxy, the agents derived from FinanceAgent, ToolRegistry, AgentMemory, AIService, AIServiceProxy, ChatGPTAdapter and the agent tools). 

- Domain and data classes (User, Account, Category, CategoryGroup, Expense, SavingsGoal, Budget, Recommendation, FinancialData) and DatabaseService. 

External systems appear as classes marked external: EmailSMSService (provided by KnockAPI) and ChatGPT5API. 

Naming note: Category is used in two ways that match the feature descriptions. In F03 a Category is a dashboard category with its own balance (Savings, Checking, Total Amount); it is the leaf of the Composite. In F04 an Expense is assigned to a Category. 

The diagram uses the following design patterns, each explained in the next section: Facade, Composite, Builder, Proxy (used twice), Adapter and Singleton. 

The class diagram is included in the Diagrams section at the end of this document. 

## Design Patterns 

Six design patterns are used, and Proxy is applied in two places. Each one solves a specific problem in the application. 

## 1. Facade - FinancialDataFacade 

Problem addressed. The budget, savings and chat features all need the same information (income, expenses, accounts, goals and budgets). Without a single entry point, every controller would have to query the database itself and know how the data is stored. 

Participating classes and roles. 

- FinancialDataFacade: the facade. Provides getFinancialData(), getBudget() and getExpenses(). 

- DatabaseService: the subsystem that stores the data. 

- BudgetController, SavingsController, GetFinancialDataTool, CompareBudgetTool, ProjectSavingsTool: clients that ask the facade for data. 

- FinancialData: the object returned to the clients. 

Why this pattern is appropriate. Clients use one simple interface. The agent tools only see the facade, so the AI model cannot reach the database directly. A change to how data is stored affects only the facade. 

What would be harder without it. Every controller and tool would repeat its own queries and depend on the database layout, so one change to a table would mean editing many classes. 

## 2. Composite - CategoryComponent, Category, CategoryGroup 

Problem addressed. F03 lets the user create dashboard categories such as savings, checking and total amount. A total is made from other categories, but the dashboard should treat a single category and a group of categories in the same way. 

Participating classes and roles. 

- CategoryComponent: the component interface (getBalance(), updateBalance()). 

- Category: the leaf, a single category with its own balance. 

- CategoryGroup: the composite. It holds child components (add(), remove()) and its getBalance() adds up their balances. 

- DashboardController: the client that works only with CategoryComponent. 

Why this pattern is appropriate. The dashboard calls getBalance() on any item and gets the right answer, whether the item is one category or a group such as Total Amount. Groups can contain other groups. 

What would be harder without it. The dashboard would need separate code for single categories and for groups, and nested groups would be difficult to support. 

## 3. Builder - SavingsGoalBuilder 

Problem addressed. A savings goal needs a name (which can be preselected), a target amount, a timeframe and a linked account. The user enters these in several steps and they must be valid before the goal exists. 

Participating classes and roles. 

- SavingsGoalBuilder: the builder (setGoalDetails(), setLinkedAccount(), build()). 

- SavingsGoal: the product that is built. 

- SavingsController: the director that calls the builder step by step. 

Why this pattern is appropriate. The goal is assembled one part at a time and only created by build() once everything is set, so an incomplete goal is never stored. 

What would be harder without it. SavingsGoal would need a long constructor with many parameters, or it could exist half-filled and be saved by mistake. 

## 4. Proxy (AI service) - AIService, AIServiceProxy 

Problem addressed. Calls to ChatGPT 5 are slow, limited and cost money. F06 and F07 also require the system to handle a slow or unavailable AI by asking the user to try again. 

Participating classes and roles. 

- AIService: the interface that all agents use (generateResponse()). 

- AIServiceProxy: the proxy. It checks access (checkAccess()), applies rate limiting and looks in its cache before passing the request on. 

- ChatGPTAdapter: the real service that the proxy delegates to. 

- FinanceAgent and its subclasses: the clients. 

Why this pattern is appropriate. Access control, rate limiting and caching are done in one place, in front of the AI model, and the agents do not need to know about them. 

What would be harder without it. Each agent would have to repeat these checks, and a missed check could allow unlimited or repeated calls. 

## 5. Proxy (two-factor authentication) - TwoFactorService, TwoFactorAuthenticationProxy 

Problem addressed. F10 requires a 6-digit code, at most 3 attempts, expiry of the code and a temporary lockout. Nothing should reach the real code check while the user is locked out. 

Participating classes and roles. 

- TwoFactorService: the interface (requestCode(), verifyCode()). 

- TwoFactorAuthenticationProxy: the proxy. It counts failedAttempts, enforces maxAttempts (3), stores lockedUntil and runs checkLockout() before passing a request on. 

- TwoFactorAuthentication: the real subject. It generates and verifies the code and sends it through EmailSMSService. 

- AuthenticationService: the client. 

Why this pattern is appropriate. The attempt limit and lockout rules sit in the proxy, and the class that creates and checks codes stays simple. 

What would be harder without it. Lockout logic would be mixed into code generation and verification, making it easier to skip by mistake. 

## 6. Adapter - ChatGPTAdapter 

Problem addressed. The ChatGPT 5 API sends and receives data in its own format, which does not match the AIService interface or the application's own objects (Recommendation, Budget). 

Participating classes and roles. 

- AIService: the target interface that the application expects. 

- ChatGPTAdapter: the adapter (generateResponse(), adaptResponse()). It builds the API request and converts the raw reply. 

- ChatGPT5API: the adaptee, the external API (chatCompletion()). 

- Recommendation and Budget: the application objects produced from the reply. 

Why this pattern is appropriate. The rest of the system depends only on AIService. If a different AI model is used later, only a new adapter is written. 

What would be harder without it. Raw API payloads would reach every agent and controller, and changing the AI model would mean changing many classes. 

7. Singleton - DatabaseService 

Problem addressed. Authentication, the dashboard, expenses, savings goals, budgets and the facade all save and load data from the same SQLite database. More than one connection could cause locking errors or conflicting writes. 

Participating classes and roles. 

- DatabaseService: keeps a single private instance and one SQLite connection. getInstance() returns it. 

- All other classes that store data use getInstance() instead of creating their own connection. 

Why this pattern is appropriate. There is one controlled access point to the database for the whole application. 

What would be harder without it. Several parts of the program could open separate connections, which can cause SQLite locking errors and inconsistent data. 

Summary 

|Pattern|Used in features|Main classes|
|---|---|---|
|Facade|F06, F07, F08, F09|FinancialDataFacade|
|Composite|F03|CategoryComponent, Category,<br>CategoryGroup|
|Builder|F05|SavingsGoalBuilder, SavingsGoal|
|Proxy|F04, F06, F07, F08 (AI); F10<br>(2FA)|AIServiceProxy; TwoFactorAuthenticationProxy|
|Adapter|F04, F06, F07, F08|ChatGPTAdapter|
|Singleton|F01 to F10|DatabaseService|



## Task 2.2: Use-Case Diagram 

The use-case diagram is included in the Diagrams section at the end of this document. 

UC1 - Create Account 

Actor(s): User, SQLite(Database) 

Goal: Allow the user to create an account for the application 

Preconditions: 

- The application is running 

- User’s email and/or phone number is not already associated with an account 

Trigger: 

- User selects “Create Account” from the login screen 

Main Success Scenario: 

- User selects Create Account 

- System displays account creation form 

- User enters email, phone number, password, password confirmation, and contact preference 

- Validates information 

- Checks if phone number and email are already in use 

- Creates user account 

- Account information is saved 

- Confirms if account was created 

## Alternative/Exception flows: 

- Required Information is missing: ask user to fill missing fields 

- Information is invalid: asks user to correct it 

- Information is already in use: system tells user to log into existing account 

Postconditions: 

- New user account exists 

- Account information is stored 

Related feature: F01 - Account creation 

UC2 - Login 

Actor(s): User, SQLite (database), KnockAPI (For 2FA) Goal: Allow user to access account Preconditions: 

- User has an existing account 

## Trigger: 

- User enters login information 

## Main Success Scenario: 

- User enters email/phone number and password 

- Checks if the key-value pair matches a result in the database 

- Request 2FA 

- Sends verification code 

- User enters verification code 

- System verifies code 

- System grants access to account 

- Dashboard is displayed 

## Alternative/Exception flows: 

- Incorrect username/password: Access is denied 

- Incorrect 2FA: User can retry 

- Three incorrect 2FA attempts: User is temporarily locked out 

- Verification code expires: User can request a new code 

Postconditions: 

- User is authenticated and can access their account 

Related feature: 

F10-2FA, F01-Account creation 

UC3 - Manage Dashboard information 

Actor(s): User 

Goal: Allows the user to manually enter and view account balance information Preconditions: 

- User is logged in 

## Trigger: 

- User opens dashboard or chooses to update their balance 

Main Success Scenario: 

- User enters an account balance 

- Checks if input is valid 

- Stores balance 

- Dashboard displays balance 

- User can view account information 

Alternative/Exception flows: 

- User enters a non-number: ask user to enter valid number 

Postconditions: 

- Account balance is stored and displayed on the dashboard 

Related feature: 

F02 - Dashboard receiving information 

UC4 - Manage Dashboard Categories 

Actor(s): User 

Goal: allows user to create categories such as checking, savings, or total amount and assign balances to them 

Preconditions: 

- User is logged in 

Trigger: 

- User selects Add Category option 

## Main Success Scenario: 

- User selects Add Category 

- User enters category name 

- User enters balance 

- System validates balance 

- Creates the category 

- Category and balance are displayed on dashboard 

## Alternative/Exception flows: 

- Invalid balance: asks user to enter valid information 

- Negative balance: accepted because the account can have a negative balance 

## Postconditions: 

- New category and balance are stored and displayed 

Related feature: 

F03 - Categories for dashboard 

UC5 - Record Monthly Expense 

Actor(s): User, ChatGPT 5 / AI Service, SQLite Goal: Allows user to record an expense and add it to a category 

Preconditions: 

- User is logged in 

- User has access to the expense section 

## Trigger: 

- User enters a new expense 

## Main Success Scenario: 

- User enters expense information 

- System sends expense information to AI 

- AI suggests an expense category 

- System displays suggested category to user 

- User confirms category 

- Expense is assigned to category 

- Expense is saved 

- Updated category information is displayed 

## Alternative/Exception flows: 

- AI cannot determine category: User manually selects a category 

- User rejects AI’s category: user selects another category 

- AI unavailable: user manually selects category 

- Invalid expense information: asks user to correct it 

Postconditions: 

- Expense is stored in a category 

Related feature: 

F04 - Monthly Expense 

UC6 - Manage Savings Goal Actor(s): User, SQLite Goal: Allow user to create and track a savings goal Preconditions: 

- User is logged in 

- User has an account that can be linked to the savings goal 

## Trigger: 

- User selects Goal Tracker from the dashboard 

## Main Success Scenario: 

- User selects Create Goal 

- User enters goal name 

- User enters target amount 

- User enters timeframe 

- User selects account to be linked to goal 

- System creates savings goal 

- System calculates current progress 

- Displays percentage and progress bar 

## Alternative/Exception flows: 

- Account balance decreases: savings progress decreases 

- Account has a negative balance: Displays decreases or “No money saved yet” 

- Invalid goal information: system asks user to correct it 

## Postconditions: 

- Savings goal is displayed 

- Current progress is displayed 

## Related feature: 

F05 - Save Goal Tracker 

UC7 - Ask AI Financial Questions 

Actor(s): User, ChatGPT 5 / AI Service 

Goal: Allow user to ask financial questions and receive AI-generated responses Preconditions: 

- User is logged in 

- AI service is available 

## Trigger: 

- User opens AI chatbox and enters a question 

## Main Success Scenario: 

- User opens AI chatbox 

- Enters a question 

- System sends question to AI 

- AI processes question 

- AI returns a response 

- System displays response in chatbox 

Alternative/Exception flows: 

- AI service is unavailable: system tells user to try again later 

- AI response takes too long: System asks user to try again 

## Postconditions: 

- AI response is displayed to user 

Related feature: 

F06 - AI Chatbox 

UC8 – Generate Budget Recommendation 

Actor(s): User, ChatGPT 5 / AI Service 

Goal: Allow the user to receive a suggested monthly or weekly budget based on their financial information and savings goal. 

Preconditions: 

- User is logged in 

- User has provided enough financial information 

- AI service is available 

## Trigger: 

- User selects the budget recommendation option and chooses a monthly or weekly budget 

## Main Success Scenario: 

- User opens the budget section 

- User selects monthly or weekly budget 

- System retrieves the user's income, expenses, and spending categories 

- System sends the financial information to the AI 

- AI analyzes the user's spending habits 

- AI generates a suggested budget 

- AI provides recommended amounts for different categories 

- System displays the suggested budget to the user 

Alternative/Exception flows: 

- There is not enough financial information: system asks the user to provide more information 

- AI service is unavailable: system tells the user to try again later 

Postconditions: 

- Suggested budget is displayed to the user 

Related feature: 

F07 - AI-powered suggested monthly and/or weekly budget 

UC9 – Generate Savings Recommendation 

Actor(s): User, ChatGPT 5 / AI Service 

Goal: Allow the user to receive a recommendation for how much they should save toward their savings goal. 

Preconditions: 

- User is logged in 

- User has a savings goal 

- User has provided the required financial information 

- AI service is available 

## Trigger: 

- User selects a savings goal and requests a savings recommendation 

## Main Success Scenario: 

- User opens their savings goal 

- User selects the option to request a savings recommendation 

- System retrieves the savings goal, target amount, current amount saved, income, and expenses 

- System sends the financial information to the AI 

- AI analyzes the user's financial information 

- AI determines a recommended weekly or monthly savings amount 

- AI generates recommendations for reaching the savings goal 

- System displays the recommendation to the user 

Alternative/Exception flows: 

- Required financial information is missing: system asks the user to provide the missing information 

- AI service is unavailable: system tells the user to try again later 

Postconditions: 

- Recommended savings amount and advice are displayed to the user 

Related feature: 

F08 - Make recommendations based on savings goal 

UC10 – Compare Budget vs. Actual Spending 

Actor(s): User, SQLite Database 

Goal: Allow the user to compare their budget with their actual spending for a selected time period. 

Preconditions: 

- User is logged in 

- User has a budget 

- User has recorded expenses 

- Budget and expense information is stored in the database 

## Trigger: 

- User opens the budget section and selects a time period 

## Main Success Scenario: 

- User opens the budget section 

- User selects a time period 

- System retrieves the user's budget from the database 

- System retrieves the user's recorded expenses from the database 

- System calculates the user's actual spending 

- System compares actual spending with the budget 

- System calculates the percentage under or over budget 

- System displays the budget, actual spending, and category differences to the user 

Alternative/Exception flows: 

- No budget exists: system tells the user that a budget is required 

- No expenses are recorded: system tells the user that no expenses have been recorded 

## Postconditions: 

- Budget and actual spending comparison is displayed to the user 

## Related feature: 

F09 - Budget vs. actual spent 

UC11 – Verify Two-Factor Authentication Actor(s): User, SQLite Database, Email/SMS Service Goal: Verify the user's identity before allowing access to their account. 

Preconditions: 

- User has an existing account 

- User has entered valid login information 

- User's email or phone number is stored in the database 

## Trigger: 

- User attempts to log into their account 

Main Success Scenario: 

- User enters their username/email and password 

- System verifies the login information using the database 

- System generates a 6-digit verification code 

- System sends the verification code to the user's email or phone 

- User enters the verification code into the application 

- System verifies the code 

- System allows the user to access their account 

## Alternative/Exception flows: 

- Incorrect verification code: system asks the user to try again 

- User enters an incorrect code three times: system temporarily blocks the user 

- Verification code expires: system allows the user to request a new code 

- Email/SMS service is unavailable: system cannot send the verification code and informs the user 

## Postconditions: 

- User is either granted or denied access to their account 

Related feature: 

F10 - 2FA 

## Task 2.3 - Sequence Diagram 

The sequence diagrams SD1 to SD6 are included in the Diagrams section at the end of this document. 

## Sequence Diagram 1 – Account Creation and Login/2FA 

Participants: 

- User 

- LoginGUI 

- AccountController 

- AuthenticationService 

- DatabaseService 

- TwoFactorAuthenticationProxy 

- TwoFactorAuthentication 

- Email/SMS Service 

- DashboardGUI 

## Main Flow – Account Creation 

- User selects Create Account on LoginGUI 

- LoginGUI sends account information to AccountController 

- AccountController sends account information to AuthenticationService 

- AuthenticationService checks DatabaseService for an existing email or phone number 

- DatabaseService returns whether the information already exists 

- AuthenticationService creates the User account 

- AuthenticationService sends the new account information to DatabaseService 

- DatabaseService saves the account 

- DatabaseService returns confirmation 

- AuthenticationService tells AccountController that the account was created 

- AccountController tells LoginGUI to display the account creation confirmation 

## Login/2FA Flow 

- User enters their email/username and password into LoginGUI 

- LoginGUI sends login information to AccountController 

- AccountController sends login information to AuthenticationService 

- AuthenticationService checks the user's information with DatabaseService 

- DatabaseService returns the user's account information 

- AuthenticationService verifies the login information 

- AuthenticationService requests a verification code from TwoFactorAuthenticationProxy 

- TwoFactorAuthenticationProxy checks that the user is not locked out and passes the request to TwoFactorAuthentication 

- TwoFactorAuthentication generates a 6-digit verification code 

- TwoFactorAuthentication sends the code to Email/SMS Service 

- Email/SMS Service sends the verification code to User 

- User enters the verification code into LoginGUI 

- LoginGUI sends the verification code to AccountController 

- AccountController sends the code to AuthenticationService 

- AuthenticationService asks TwoFactorAuthenticationProxy to verify the code 

- TwoFactorAuthenticationProxy checks the lockout and the attempt limit (3 attempts) and asks TwoFactorAuthentication to verify the code 

- TwoFactorAuthentication returns the verification result to TwoFactorAuthenticationProxy, which passes it to AuthenticationService 

- AuthenticationService tells AccountController whether authentication was successful 

- AccountController tells DashboardGUI to display the dashboard if authentication is successful 

## Alternative Flow 

- User enters invalid account information during account creation: system asks the user to correct the information 

- Email or phone number already exists: system tells the user to log into the existing account 

- User enters incorrect username/password: system denies login 

- User enters an incorrect 2FA code: system asks the user to try again 

- User enters an incorrect 2FA code three times: system temporarily blocks the user 

- 2FA code expires: system allows the user to request a new verification code 

- Email/SMS service is unavailable: system informs the user that the verification code could not be sent 

## Sequence Diagram 2 – Dashboard and Expense Management 

## Participants: 

- User 

- DashboardGUI 

- DashboardController 

- ExpenseGUI 

- ExpenseController 

- Account 

- Category 

- Expense 

- ExpenseCategorizerAI 

- AIServiceProxy 

- DatabaseService 

## Main Flow – Dashboard Information 

- User enters their account balance into DashboardGUI 

- DashboardGUI sends the balance to DashboardController 

- DashboardController sends the balance to Account 

- Account updates the balance 

- DashboardController sends the updated Account to DatabaseService 

- DatabaseService saves the updated account information 

- DatabaseService returns confirmation 

- DashboardController retrieves the updated account information 

- DashboardController tells DashboardGUI to display the updated balance 

## Category Flow 

- User selects Add Category on DashboardGUI 

- User enters the category name and balance 

- DashboardGUI sends the category information to DashboardController 

- DashboardController creates the Category 

- DashboardController sends the Category to DatabaseService 

- DatabaseService saves the Category 

- DatabaseService returns confirmation 

- DashboardController tells DashboardGUI to display the new category 

## Expense Flow 

- User opens Monthly Expense through ExpenseGUI 

- User enters expense information 

- ExpenseGUI sends the expense to ExpenseController 

- ExpenseController sends the expense information to ExpenseCategorizerAI 

- ExpenseCategorizerAI sends the request to AIServiceProxy, which forwards it to ChatGPT 5 and returns the response 

- ExpenseCategorizerAI returns a suggested Category to ExpenseController 

- ExpenseController displays the suggested category through ExpenseGUI 

- User confirms the category 

- ExpenseController creates the Expense 

- ExpenseController assigns the Category to the Expense 

- ExpenseController sends the Expense to DatabaseService 

- DatabaseService saves the Expense 

- DatabaseService returns confirmation 

- ExpenseController tells ExpenseGUI to display the updated expense 

## Alternative Flow 

- User enters an invalid balance: system asks the user to enter a valid number 

- User enters an invalid category balance: system asks the user to enter valid information 

- AI cannot determine an expense category: system allows the user to manually select a category 

- User disagrees with the AI category: system allows the user to select a different category 

- AI service is unavailable: system allows the user to manually categorize the expense 

- Database cannot save the information: system informs the user that the information could not be saved 

## Sequence Diagram 3 – Savings Goal and Savings Recommendation 

## Participants: 

- User 

- SavingsGoalGUI 

- SavingsController 

- SavingsGoalBuilder 

- SavingsGoal 

- Account 

- FinancialDataFacade 

- SavingsAI 

- AIServiceProxy 

- DatabaseService 

## Main Flow – Create Savings Goal 

- User selects Goal Tracker on SavingsGoalGUI 

- User enters the goal name, target amount, and timeframe 

- User selects the account linked to the goal 

- SavingsGoalGUI sends the goal information to SavingsController 

- SavingsController instantiates and invokes SavingsGoalBuilder 

- SavingsGoalBuilder calls setGoalDetails and setLinkedAccount 

- SavingsGoalBuilder calls build to instantiate the SavingsGoal object 

- SavingsController sends the SavingsGoal to DatabaseService 

- DatabaseService saves the SavingsGoal 

- DatabaseService returns confirmation 

- SavingsController asks SavingsGoal to calculate the current progress, which reads the balance of the linked Account 

- SavingsController tells SavingsGoalGUI to display the savings goal and progress 

## Savings Recommendation Flow 

- User selects their savings goal 

- User selects Request Recommendation 

- SavingsGoalGUI sends the request to SavingsController 

- SavingsController asks FinancialDataFacade for the user's financial information 

- FinancialDataFacade retrieves the required account, expense, and savings goal information from DatabaseService 

- DatabaseService returns the financial information 

- FinancialDataFacade returns the information to SavingsController 

- SavingsController sends the financial information to SavingsAI 

- SavingsAI calculates the monthly savings target (calculateMonthlyTarget()) 

- SavingsAI sends the request to AIServiceProxy, which forwards it to ChatGPT 5 

- ChatGPT 5 generates a savings recommendation and advice 

- AIServiceProxy returns the AI response to SavingsAI 

- SavingsAI returns the recommendation to SavingsController 

- SavingsController sends the recommendation to SavingsGoalGUI 

- SavingsGoalGUI displays the recommended savings amount and advice 

## Alternative Flow 

- User does not provide a required savings goal: system asks the user to create a savings goal 

- Required financial information is missing: system asks the user to provide the missing information 

- Account balance decreases: system updates the savings goal progress 

- Account has a negative balance: system reduces the progress or displays "no money saved yet" 

- AI service is unavailable: system tells the user to try again later 

- Database cannot retrieve financial information: system informs the user that the information could not be retrieved 

Sequence Diagram 4 – AI Budget Recommendation 

Participants: 

- User 

- BudgetGUI 

- BudgetController 

- FinancialDataFacade 

- Budget 

- BudgetAI 

- AIServiceProxy 

- ChatGPTAdapter 

- ChatGPT 5 API 

- DatabaseService 

## Main Flow 

- User opens the Budget section through BudgetGUI 

- User selects a monthly or weekly budget recommendation 

- BudgetGUI sends the request to BudgetController 

- BudgetController asks FinancialDataFacade for the user's financial information 

- FinancialDataFacade requests the user's information from DatabaseService 

- DatabaseService retrieves the user's income, expenses, categories, and savings goal information 

- DatabaseService returns the financial information to FinancialDataFacade 

- FinancialDataFacade returns the financial information to BudgetController 

- BudgetController sends the financial information to BudgetAI 

- BudgetAI calculates a baseline budget from the user's financial information (calculateBaseline()) 

- BudgetAI sends the request to AIServiceProxy 

- AIServiceProxy checks access and the rate limit and passes the request to ChatGPTAdapter 

- ChatGPTAdapter sends the request to the ChatGPT 5 API 

- ChatGPT 5 generates a recommended budget 

- ChatGPTAdapter converts the raw response into a Recommendation 

- AIServiceProxy returns the Recommendation to BudgetAI 

- BudgetAI returns the recommended budget to BudgetController 

- BudgetController creates/displays the Budget recommendation 

- BudgetController sends the recommendation to BudgetGUI 

- BudgetGUI displays the suggested budget and category amounts to the User 

## Alternative Flow 

- User does not have enough financial information: system asks the user to provide more information 

- User has no recorded expenses: system asks the user to enter expense information 

- AI service is unavailable: system tells the user to try again later 

- Database cannot retrieve the required information: system informs the user that the financial information could not be retrieved 

Sequence Diagram 5 – Budget vs. Actual Spending 

Participants: 

- User 

- BudgetGUI 

- BudgetController 

- FinancialDataFacade 

- Budget 

- Expense 

- DatabaseService 

## Main Flow 

- User opens the Budget section through BudgetGUI 

- User selects Budget vs. Actual Spending 

- User selects a time period 

- BudgetGUI sends the selected period to BudgetController 

- BudgetController asks FinancialDataFacade for the budget for the selected period 

- FinancialDataFacade requests the budget from DatabaseService 

- DatabaseService returns the budget 

- FinancialDataFacade returns the budget to BudgetController 

- BudgetController asks FinancialDataFacade for recorded expenses 

- FinancialDataFacade requests the expenses from DatabaseService 

- DatabaseService returns the recorded expenses 

- FinancialDataFacade returns the expenses to BudgetController 

- BudgetController calculates the actual spending 

- BudgetController compares the actual spending with the Budget 

- BudgetController calculates the difference between the budget and actual spending 

- BudgetController calculates the percentage under or over budget 

- BudgetController sends the results to BudgetGUI 

- BudgetGUI displays the budget, actual spending, percentages, and category differences to the User 

## Alternative Flow 

- No budget exists: system tells the user that a budget is required 

- No expenses are recorded: system tells the user that no expenses have been recorded 

- Database cannot retrieve the budget: system informs the user that the budget could not be retrieved 

- Database cannot retrieve expenses: system informs the user that the expense information could not be retrieved 

## Sequence Diagram 6 – AI Chatbox 

## Participants: 

- User 

- ChatGUI 

- AIController 

- ChatAI 

- AgentMemory 

- AIServiceProxy 

- ChatGPTAdapter 

- ChatGPT 5 API 

- ToolRegistry 

- GetFinancialDataTool 

- FinancialDataFacade 

## Main Flow 

- User opens the AI Chatbox 

- User enters a financial question into ChatGUI 

- ChatGUI sends the question to AIController 

- AIController sends the question to ChatAI 

- ChatAI recalls earlier messages and preferences from AgentMemory and builds the prompt 

- ChatAI passes the request to AIServiceProxy 

- AIServiceProxy performs access control, rate limiting and a cache check 

- AIServiceProxy delegates the request to ChatGPTAdapter 

- ChatGPTAdapter sends the request to the ChatGPT 5 API 

- ChatGPT 5 processes the question and either answers or asks for a tool (for example, the user's financial data) 

- ChatGPTAdapter converts the raw response and returns it to ChatAI through AIServiceProxy 

- If a tool is requested, ChatAI asks ToolRegistry to execute it, and ToolRegistry runs GetFinancialDataTool 

- GetFinancialDataTool asks FinancialDataFacade for the user's financial information, and the result is returned to ChatAI 

- ChatAI stores the result in AgentMemory and sends the request to AIServiceProxy again. This repeats until ChatGPT 5 gives a final answer or the maximum number of steps is reached 

- ChatAI stores the final answer in AgentMemory and returns it to AIController 

- AIController sends the response to ChatGUI 

- ChatGUI displays the AI response to the User 

## Alternative Flow 

- AI service is unavailable: system tells the user to try again later 

- AI response takes too long: system tells the user to try again 

- AI returns an invalid or unusable response: system informs the user that a response could not be generated 

- Maximum number of steps is reached: the agent answers with the information it has and tells the user the answer may be incomplete 

Task 3: Feature-To-Design Traceability 

|Feature|Description|Type|Related Use<br>Case|Classes|Key Methods|Sequence<br>Diagram|Design<br>Patterns|
|---|---|---|---|---|---|---|---|
|F01|Account<br>Creation|Deterministic|UC1 - Create<br>Account|LoginGUI,<br>AccountController,<br>AuthenticationService, User,<br>DatabaseService|createAccount(),<br>validateAccountInfo(),<br>saveUser()|SD1 -<br>Account<br>Creation and<br>Login/2FA|Singleton|
|F02|Dashboard<br>Receiving<br>Information|Deterministic|UC3 - Manage<br>Dashboard<br>Information|DashboardGUI,<br>DashboardController,<br>Account, DatabaseService|updateBalance(),<br>getBalance(),<br>saveAccount()|SD2 -<br>Dashboard<br>and<br>Expense Ma<br>nagement|Singleton|
|F03|Categories for<br>Dashboard|Deterministic|UC4 - Manage<br>Dashboard<br>Categories|DashboardGUI,<br>DashboardController,<br>CategoryComponent,<br>CategoryGroup, Category,<br>DatabaseService|createCategory(),<br>updateCategoryBalance(),<br>add(), remove(),<br>saveCategory()|SD2 -<br>Dashboard<br>and<br>Expense Ma<br>nagement|Composite,<br>Singleton|
|F04|Monthly<br>Expense|Hybrid|UC5 - Record<br>Monthly<br>Expense|ExpenseGUI,<br>ExpenseController,<br>Expense, Category,<br>ExpenseCategorizerAI,<br>AIServiceProxy, AIService,<br>DatabaseService|recordExpense(),<br>categorizeExpense(),<br>assignCategory(),<br>saveExpense()|SD2 -<br>Dashboard<br>and<br>Expense Ma<br>nagement|Proxy,<br>Singleton|
|F05|Save Goal<br>Tracker|Deterministic|UC6 - Manage<br>Savings Goal|SavingsGoalGUI,<br>SavingsController,<br>SavingsGoalBuilder,<br>SavingsGoal, Account,<br>DatabaseService|submitGoal(),<br>createGoal(),<br>setGoalDetails(),<br>setLinkedAccount(),<br>build(),<br>calculateProgress(),<br>saveGoal()|SD3 -<br>Savings<br>Goal and<br>Savings Rec<br>ommendatio<br>n|Builder,<br>Singleton|



|Feature|Description|Type|Related Use<br>Case|Classes|Key Methods|Sequence<br>Diagram|Design<br>Patterns|
|---|---|---|---|---|---|---|---|
|F06|AI Chatbox|AI-Based|UC7 - Ask AI<br>Financial<br>Questions|ChatGUI, AIController,<br>ChatAI, AgentMemory,<br>ToolRegistry,<br>GetFinancialDataTool,<br>FinancialDataFacade,<br>AIServiceProxy,<br>ChatGPTAdapter, AIService|submitQuestion(),<br>askQuestion(),<br>processQuestion(), run(),<br>execute(),<br>generateResponse()|SD6 - AI<br>Chatbox|Proxy,<br>Adapter,<br>Facade|
|F07|AI-Powered<br>Suggested Mo<br>nthly/Weekly<br>Budget|AI-Based|UC8 -<br>Generate<br>Budget Recom<br>mendation|BudgetGUI,<br>BudgetController,<br>FinancialDataFacade,<br>BudgetAI, AIServiceProxy,<br>ChatGPTAdapter, Budget,<br>Recommendation,<br>DatabaseService|requestBudget(),<br>getFinancialData(),<br>generateBudget(),<br>calculateBaseline(),<br>adaptResponse(),<br>createBudget()|SD4 - AI<br>Budget Reco<br>mmendation|Facade,<br>Proxy,<br>Adapter|
|F08|Recommendati<br>ons based on<br>Savings Goal|AI-Based|UC9 -<br>Generate<br>Savings Reco<br>mmendation|SavingsGoalGUI,<br>SavingsController,<br>FinancialDataFacade,<br>SavingsAI, AIServiceProxy,<br>SavingsGoal,<br>Recommendation,<br>DatabaseService|requestRecommendation(<br>), generateSavingsRecom<br>mendation(),<br>getFinancialData(),<br>analyzeSavingsGoal(),<br>calculateMonthlyTarget()|SD3 -<br>Savings<br>Goal and<br>Savings Rec<br>ommendatio<br>n|Facade,<br>Proxy|
|F09|Budget vs.<br>Actual Spent|Deterministic|UC10 -<br>Compare<br>Budget vs<br>Actual<br>Spending|BudgetGUI,<br>BudgetController,<br>FinancialDataFacade,<br>Budget, Expense,<br>DatabaseService|getBudget(),<br>getExpenses(),<br>calculateActual(),<br>calculateDifference(),<br>compareBudgetToActual()|SD5 -<br>Budget vs<br>Actual<br>Spending|Facade|
|F10|Two-Factor<br>Authentication|Deterministic|UC11 - Verify<br>Two-Factor<br>Authentication|LoginGUI,<br>AccountController,<br>AuthenticationService, TwoF<br>actorAuthenticationProxy,<br>TwoFactorAuthentication,<br>DatabaseService,<br>EmailSMSService|submitLogin(), login(),<br>verifyCredentials(),<br>generateCode(),<br>sendCode(), verifyCode()|SD1 -<br>Account<br>Creation and<br>Login/2FA|Proxy,<br>Singleton|



Patterns are limited to those covered in the course: Facade, Composite, Builder, Proxy, Adapter and Singleton. Singleton refers to DatabaseService, which every feature that stores data uses. The agent classes for F04, F06, F07 and F08 (ExpenseCategorizerAI, ChatAI, BudgetAI, SavingsAI) extend FinanceAgent; see Agent Behavior. 

Task 4 - Explain How Each Feature Is Realized 

## F01 - Account Creation 

Related Use Case: UC1 - Create Account 

Related Sequence Diagram: SD1 - Account Creation and Login/2FA 

## Classes involved 

- LoginGUI: collects the user's email, phone number, password, password confirmation, and contact preference. 

- AccountController: receives the account-creation request from the GUI. 

- AuthenticationService: validates the account information and handles account creation. 

- User: represents the newly created user account. 

- DatabaseService: saves the account information to SQLite. 

## Important methods 

- LoginGUI.submitAccountInfo() 

- AccountController.createAccount() 

- AuthenticationService.validateAccountInfo() 

- AuthenticationService.createAccount() 

- DatabaseService.saveUser() 

## Execution 

When the user selects Create Account, LoginGUI collects the required account information and sends it to AccountController. The controller passes the information to AuthenticationService, which validates the information and checks DatabaseService to determine whether the email or phone number already exists. If the information is valid, a User object is created and stored in the SQLite database. The result is then returned to the GUI, and the account-creation confirmation is displayed. 

## F02 - Dashboard Receiving Information 

Related Use Case: UC3 - Manage Dashboard Information 

Related Sequence Diagram: SD2 - Dashboard and Expense Management 

## Classes involved 

- DashboardGUI: allows the user to enter and view their balance. 

- DashboardController: coordinates the dashboard request. 

- Account: stores the user's account balance. 

- DatabaseService: saves and retrieves account information. 

## Important methods 

- DashboardGUI.submitBalance() 

- DashboardController.updateBalance() 

- Account.updateBalance() 

- Account.getBalance() 

## -   DatabaseService.saveAccount() 

## Execution 

The user enters their account balance through DashboardGUI. The GUI sends the information to DashboardController, which updates the appropriate Account object. The updated account information is saved through DatabaseService. The controller then retrieves the updated information and sends it back to the GUI so that the new balance is displayed on the dashboard. 

## F03 - Categories for Dashboard 

Related Use Case: UC4 - Manage Dashboard Categories 

Related Sequence Diagram: SD2 - Dashboard and Expense Management 

## Classes involved 

- DashboardGUI: allows the user to add and enter information for a category. 

- DashboardController: processes category requests. 

- Category: represents a dashboard category such as Savings, Checking, or Total Amount. 

- DatabaseService: stores the category and balance. 

- CategoryComponent: Abstract base component declaring common category operations 

- Category: leaf category object representing an individual category 

- CategoryGroup: Composite category containing child items to form hierarchical categories 

## Important methods 

- DashboardGUI.addCategory() 

- DashboardController.createCategory() 

- Category.updateBalance() 

- DatabaseService.saveCategory() 

- CategoryComponent.updateBalance() 

- CategoryGroup.add() 

## Execution 

The user selects "Add Category" and inputs the category name and balance. DashboardGUI forwards the request to DashboardController. The system utilizes the Composite Pattern, treating individual sub-categories (Category) and aggregated group collections (CategoryGroup) uniformly through CategoryComponent. The constructed structure is saved via DatabaseService and rendered on DashboardGUI. 

## F04 - Monthly Expense 

Related Use Case: UC5 - Record Monthly Expense 

Related Sequence Diagram: SD2 - Dashboard and Expense Management 

## Classes involved 

- ExpenseGUI: collects the expense information from the user. 

- ExpenseController: coordinates expense recording and categorization. 

- Expense: represents the recorded expense. 

- Category: represents the category assigned to the expense. 

- ExpenseCategorizerAI: an AI agent that analyzes the expense and suggests a category. 

- AIServiceProxy: checks access and rate limits before the request reaches ChatGPT 5. 

- DatabaseService: stores the expense. 

## Important methods 

- ExpenseGUI.submitExpense() 

- ExpenseController.recordExpense() 

- ExpenseCategorizerAI.categorizeExpense() 

- ExpenseController.assignCategory() 

- DatabaseService.saveExpense() 

## Execution 

The user enters a monthly expense through ExpenseGUI. ExpenseController sends the expense information to ExpenseCategorizerAI, which sends the request to ChatGPT 5 through AIServiceProxy and returns a suggested category. The suggested category is presented to the user, who can either confirm it or manually select another category. The controller then creates an Expense object, associates it with the selected Category, and saves it through DatabaseService. This is a hybrid feature because the expense is entered and stored deterministically, while AI assists with categorization. The original specification explicitly allows the user to manually override the AI's suggested category. 

## F05 - Save Goal Tracker 

Related Use Case: UC6 - Manage Savings Goal 

Related Sequence Diagram: SD3 - Savings Goal and Savings Recommendation 

## Classes involved 

- SavingsGoalGUI: collects the savings goal information. 

- SavingsController: coordinates goal creation. 

- SavingsGoal: stores the goal name, target amount, timeframe, and progress. 

- Account: represents the account linked to the goal. 

- DatabaseService: saves the goal information. 

- SavingsGoalBuilder: constructs SavingsGoal object using specified parameters 

## Important methods 

- SavingsGoalGUI.submitGoal() 

- SavingsController.createGoal() 

- SavingsController.linkAccount() 

- SavingsGoal.calculateProgress() 

- DatabaseService.saveGoal() 

- SavingsGoalBuilder.setGoalDetails() 

- SavingsGoalBuilder.build() 

## Execution 

The user opens the goal tracker and inputs a goal name, target amount, timeframe, and linked account. SavingsGoalGUI passes these parameters to SavingsController. SavingsController uses SavingsGoalBuilder to step-by-step set goal details and link the Account. Calling build() returns the fully constructed SavingsGoal object, which is persisted via DatabaseService. The progress percentage is computed and updated on SavingsGoalGUI. 

## F06 - AI Chatbox 

Related Use Case: UC7 - Ask AI Financial Questions 

Related Sequence Diagram: SD6 - AI Chatbox 

## Classes involved 

- ChatGUI: provides the chatbox and collects the user's question. 

- AIController: coordinates the AI request. 

- ChatAI: handles financial chat interactions. 

- AIService: communicates with ChatGPT 5. 

- AIServiceProxy: acts as a proxy for AIService to handle rate limiting, access control and caching. 

- FinanceAgent, AgentMemory, ToolRegistry: ChatAI extends FinanceAgent, which keeps the conversation in AgentMemory and runs tools such as GetFinancialDataTool through ToolRegistry. 

- ChatGPTAdapter: translates between the application and the ChatGPT 5 API. 

## Important methods 

- ChatGUI.submitQuestion() 

- AIController.askQuestion() 

- ChatAI.processQuestion() 

- AIService.generateResponse() 

- AIServiceProxy.generateResponse() 

- FinanceAgent.run() 

- ToolRegistry.execute() 

## Execution 

The user submits a prompt via ChatGUI. AIController passes it to ChatAI, which recalls earlier messages from AgentMemory, builds the prompt and sends it to AIServiceProxy. The proxy checks access, applies rate limits and looks in its cache. On a cache miss it forwards the request to ChatGPTAdapter, which calls ChatGPT 5 and converts the raw reply. If ChatGPT 5 asks for the user's financial data, ChatAI runs GetFinancialDataTool through ToolRegistry (the tool reads data through FinancialDataFacade), stores the result in AgentMemory and asks again. This repeats until a final answer is returned or the maximum number of steps is reached. The answer travels back through AIController to display on ChatGUI. 

## F07 - AI-Powered Suggested Monthly/Weekly Budget 

Related Use Case: UC8 - Generate Budget Recommendation Related Sequence Diagram: SD4 - AI Budget Recommendation 

## Classes involved 

- BudgetGUI: allows the user to request a monthly or weekly budget. 

- BudgetController: coordinates the request. 

- FinancialDataFacade: retrieves the financial information required by the AI. 

- BudgetAI: analyzes the user's financial information. 

- AIServiceProxy: checks access and rate limits before the AI request is sent. 

- Recommendation: holds the suggested budget and the advice returned by the AI. 

- Budget: represents the generated budget. 

- DatabaseService: provides stored financial information. 

- ChatGPTAdapter: Implements the adapter pattern ot translate raw external AI payloads into internal application domain models 

## Important methods 

- BudgetGUI.requestBudget() 

- BudgetController.generateBudget() 

- FinancialDataFacade.getFinancialData() 

- BudgetAI.analyzeFinances() 

- BudgetAI.calculateBaseline() 

- Budget.createBudget() 

- ChatGPTAdapter.adaptResponse() 

## Execution 

The user selects a weekly or monthly budget recommendation in BudgetGUI. BudgetController retrieves user financials via FinancialDataFacade and forwards them to BudgetAI. BudgetAI calculates a baseline budget (calculateBaseline()) and sends the request to ChatGPT 5 through AIServiceProxy. ChatGPTAdapter calls the ChatGPT 5 API and processes the response into a structured Recommendation, which is used to create the Budget and is displayed back to the user via BudgetGUI. 

## F08 - Recommendations Based on Savings Goal 

Related Use Case: UC9 - Generate Savings Recommendation 

Related Sequence Diagram: SD3 - Savings Goal and Savings Recommendation 

## Classes involved 

- SavingsGoalGUI: allows the user to request a recommendation. 

- SavingsController: coordinates the request. 

- FinancialDataFacade: retrieves the user's financial information. 

- SavingsAI: analyzes the savings goal and financial situation. 

- AIServiceProxy: checks access and rate limits before the AI request is sent. 

- Recommendation: holds the recommended savings amount and advice. 

- SavingsGoal: stores the target and current progress. 

- DatabaseService: provides the stored financial information. 

## Important methods 

- SavingsGoalGUI.requestRecommendation() 

- SavingsController.generateSavingsRecommendation() 

- FinancialDataFacade.getFinancialData() 

- SavingsAI.analyzeSavingsGoal() 

- SavingsAI.calculateMonthlyTarget() 

## Execution 

The user selects a goal and requests a recommendation. SavingsController retrieves target amounts and current income/expenses using FinancialDataFacade. SavingsAI calculates the weekly/monthly saving target (calculateMonthlyTarget()) and sends the request to ChatGPT 5 through AIServiceProxy. The recommendation and actionable feedback are returned to SavingsGoalGUI. 

## F09 - Budget vs Actual Spent 

Related Use Case: UC10 - Compare Budget vs Actual Spending 

Related Sequence Diagram: SD5 - Budget vs Actual Spending 

## Classes involved 

- BudgetGUI: allows the user to select the period and view the comparison. 

- BudgetController: performs the comparison. 

- FinancialDataFacade: retrieves the required financial data. 

- Budget: contains the planned budget. 

- Expense: contains recorded spending. 

- DatabaseService: retrieves the stored data. 

## Important methods 

- BudgetGUI.selectPeriod() 

- BudgetController.compareBudgetToActual() 

- FinancialDataFacade.getBudget() 

- FinancialDataFacade.getExpenses() 

- BudgetController.calculateActual() 

- BudgetController.calculateDifference() 

## Execution 

The user selects a budget period from BudgetGUI. The request is sent to BudgetController, which uses FinancialDataFacade to retrieve the budget and recorded expenses from DatabaseService. The controller calculates the user's actual spending from the recorded Expense objects. It then compares the actual spending against the Budget and calculates the difference and percentage under or over budget. The results, including category-level differences, are sent back to BudgetGUI and displayed to the user. This directly implements F09, which requires the system to retrieve the budget and recorded expenses, calculate actual spending, and compare the two. 

## F10 - Two-Factor Authentication 

Related Use Case: UC11 - Verify Two-Factor Authentication Related Sequence Diagram: SD1 - Account Creation and Login/2FA 

## Classes involved 

- LoginGUI: collects login credentials and the 2FA code. 

- AccountController: coordinates login requests. 

- AuthenticationService: verifies account credentials and coordinates authentication. 

- TwoFactorAuthenticationProxy: uses the Proxy pattern as a security gatekeeper, blocking access to the protected application until the user is successful at entering the correct verification code. 

- TwoFactorAuthentication: generates and verifies the 6-digit code. 

- DatabaseService: retrieves the user's stored account/contact information. 

- Email/SMS Service: sends the verification code to the user. 

## Important methods 

- LoginGUI.submitLogin() 

- AccountController.login() 

- AuthenticationService.verifyCredentials() 

- TwoFactorAuthentication.generateCode() 

- TwoFactorAuthentication.sendCode() 

- TwoFactorAuthenticationProxy.verifyCode() 

## Execution 

Upon credential verification via AuthenticationService, TwoFactorAuthenticationProxy intercepts system routing and triggers code generation via TwoFactorAuthentication. EmailSMSService sends a 6-digit passcode to the user. TwoFactorAuthenticationProxy evaluates user input, enforcing attempt thresholds and expiration timeouts before permitting navigation to the main application dashboard 

Task 2.1: Class Diagram - AI Personal Finance Assistant 



<!-- Start of picture text -->
LoginGUI DashboardGUI ExpenseGUI SavingsGoalGUI BudgetGUI ChatGUI<br>-controller: AccountController -controller: DashboardController -controller: ExpenseController -controller: SavingsController -controller: BudgetController -controller: AIController<br>+submitAccountInfo() +submitBalance() +submitExpense() +submitGoal() +requestBudget() +submitQuestion()<br>+submitLogin() +addCategory() +confirmCategory() +requestRecommendation() +selectPeriod()<br>+submitCode() 1<br>1 1 1 1<br>1<br>1 1 1 1 1 1<br>AccountController DashboardController ExpenseController SavingsController BudgetController AIController<br>+createAccount(info) +updateBalance(acct, amt) +recordExpense(data) +createGoal(details) +generateBudget(period) +askQuestion(text)<br>+login(id, pw) +createCategory(name, bal) +assignCategory(exp, cat) +linkAccount(acct) +compareBudgetToActual(period)<br>+verifyCode(code) +generateSavingsRecommendation(id) +calculateActual() 1<br>0..* 1 0..* 1 +calculateDifference()<br>1 1 0..*1<br>0..*1<br>1 1 1 1 1 1 1<br>AuthenticationService ExpenseCategorizerAI <<Builder>> SavingsAI ChatAI BudgetAI <<Facade>><br>SavingsGoalBuilder FinancialDataFacade<br>+validateAccountInfo(info) +categorizeExpense(expense) +analyzeSavingsGoal(goal, data) +processQuestion(text) +analyzeFinances(data)<br>+createAccount(info) +setGoalDetails(name, target, time) +calculateMonthlyTarget(goal, data) +calculateBaseline(data) +getFinancialData(userId)<br>+verifyCredentials(id, pw) +setLinkedAccount(acct) +getBudget(userId, period)<br>+verifyCode(user, code) +build(): SavingsGoal +getExpenses(userId, period)<br>1 0..* 0..*<br>1<br><<build>><br>1 0..*<br><<interface>> <<Composite>> interface SavingsGoal <<abstract>> Recommendation FinancialData<br>TwoFactorService CategoryComponent FinanceAgent<br>-name -period -accounts<br>-targetAmount -aiService: AIService -suggestedAmounts: Map -expenses<br>-timeframe -memory: AgentMemory -advice: String -income<br>+requestCode(user) +getBalance() -linkedAccount -tools: ToolRegistry -goals<br>+verifyCode(user, code) +updateBalance(amt) -maxSteps: int -budget<br>+calculateProgress()<br>0..* +run(input)<br><<create>> 0..* 0..* #buildPrompt(input)<br>#parseResult(response)<br><<create>><br>0..* 1<br>1<br><<create>><br>1 1 1 1<br>1<br><<Proxy>> <<leaf>> <<composite>> Account <<interface>> AgentMemory ToolRegistry<br>TwoFactorAuthenticationProxy Category CategoryGroup AIService<br>-accountId -messages -tools: Map<br>-failedAttempts: int -name -name -name -preferences<br>-maxAttempts = 3 -balance -children -balance +register(tool)<br>-lockedUntil +generateResponse(request): AIResponse +add(entry) +execute(name, args)<br>+updateBalance(amt) +add(c) +updateBalance(amt) +recall(userId)<br>+requestCode(user) +getBalance() +remove(c) +getBalance()<br>+verifyCode(user, code) +getBalance() 1 1<br>-checkLockout()<br>1 0..*<br>1<br>delegates<br>real subject<br>1 1<br>1 1 0..*<br>TwoFactorAuthentication <<Singleton>> User <<Adapter>> <<Proxy>> <<interface>><br>DatabaseService ChatGPTAdapter AIServiceProxy AgentTool<br>-expiryMinutes -userId<br>-codeLength = 6 -instance: DatabaseService -email -api: ChatGPT5API -rateLimiter<br>-connection: SQLite -phone -cache<br>+requestCode(user) -passwordHash +generateResponse(request) -realService: AIService +getName()<br>+verifyCode(user, code) +getInstance() -contactPreference +adaptResponse(raw) +execute(args): ToolResult<br>-generateCode() +saveUser() +generateResponse(request)<br>-sendCode() +saveAccount() +getContact() 0..* -checkAccess()<br>+saveCategory()<br>0..* +saveExpense() 1 1<br>+saveGoal()<br>+saveBudget()<br><<adapts>><br>1 0..* 0..* 1<br>0..* 0..* 0..*<br><<external>> Expense Budget <<external>> CompareBudgetTool ProjectSavingsTool<br>EmailSMSService ChatGPT5API<br>-amount -period<br>-date -totalAmount<br>-description -categoryLimits: Map +execute(args) +execute(args)<br>+send(contact, message) -category +chatCompletion(payload)<br>+createBudget(rec)<br>+getCategory() +getLimit(category)<br><!-- End of picture text -->



<!-- Start of picture text -->
0..*<br>GetFinancialDataTool<br>+execute(args)<br><!-- End of picture text -->



<!-- Start of picture text -->
Notation Multiplicity Design patterns used<br>Association 1 Exactly one Facade: FinancialDataFacade gives controllers and agent tools one interface to the stored financial data.<br>Composite: CategoryComponent (component), Category (leaf), CategoryGroup (composite).<br>Inheritance 0..* Zero or more<br>Builder: SavingsGoalBuilder builds SavingsGoal step by step.<br>Realization / Implementation 1..* One or more Proxy: AIServiceProxy (access control, rate limit, cache) and TwoFactorAuthenticationProxy (attempt limit, lockout).<br>Adapter: ChatGPTAdapter makes the ChatGPT 5 API fit the AIService interface and the domain objects.<br>Dependency 0..1 Zero or one Singleton: DatabaseService (one SQLite connection).<br>Aggregation 2..7 Specified range<br>Composition<br><!-- End of picture text -->

Task 2.2: Use-Case Diagram - AI Personal Finance Assistant 



<!-- Start of picture text -->
UC1 Create Account<br>UC2 Login<br><<include>><br>UC11 Verify Two-Factor<br>Authentication<br>Email/SMS Service<br>(KnockAPI)<br>UC3 Manage Dashboard<br>Information<br>UC4 Manage Dashboard<br>Categories<br>SQLite Database<br>UC6 Manage Savings Goal<br>User<br>UC10 Compare Budget vs Actual<br>Spending<br>UC5 Record Monthly Expense<br>UC7 Ask AI Financial Questions<br>UC8 Generate Budget<br>Recommendation AI Service<br>(ChatGPT 5)<br>UC9 Generate Savings<br>Recommendation<br>System boundary: AI Personal Finance Assistant<br><!-- End of picture text -->

|Feature coverage||||
|---|---|---|---|
|F01 Account Creation|-> UC1|F06 AI chatbox|-> UC7|
|F02 Dashboard receiving information|-> UC3|F07 AI-powered suggested budget|-> UC8|
|F03 Categories for dashboard|-> UC4|F08 Recommendations based on savings goal|-> UC9|
|F04 Monthly Expense|-> UC5|F09 Budget vs. actual spent|-> UC10|
|F05 Save goal tracker|-> UC6|F10 2FA|-> UC11|



Actors: User (primary); ChatGPT 5 / AI Service, Email/SMS Service (KnockAPI) and SQLite Database (external). UC2 Login includes UC11. 

Task 2.3 - Sequence Diagram 1 Account Creation and Login with 2FA (F01, F10) 



<!-- Start of picture text -->
TwoFactorAuthentication<br>User LoginGUI AccountController AuthenticationService DatabaseService TwoFactorAuthentication EmailSMSService DashboardGUI<br>Proxy<br>submitAccountInfo(email, phone, password, confirm, contactPref)<br>createAccount(info)<br>validateAccountInfo(info)<br>findUser(email, phone)<br>exists or not<br>createAccount(info)<br>saveUser(user)<br>confirmation<br>accountCreated<br>showMessage(account created)<br>submitLogin(identifier, password)<br>login(identifier, password)<br>verifyCredentials(identifier, password)<br>findUser(identifier)<br>User<br>requestCode(user)<br>requestCode(user)<br>generateCode()<br>sendCode(contact, code)<br>6-digit code by email or SMS<br>submitCode(code)<br>verifyCode(code)<br>verifyCode(user, code)<br>verifyCode(user, code)<br>checkLockout()<br>verifyCode(user, code)<br>AuthResult<br>AuthResult<br>AuthResult<br>displayDashboard()<br><!-- End of picture text -->

Solid arrow = call.  Dashed arrow = return. 

Task 2.3 - Sequence Diagram 2 Dashboard and Expense Management (F02, F03, F04) 



<!-- Start of picture text -->
User DashboardGUI DashboardController Account Category DatabaseService ExpenseGUI ExpenseController ExpenseCategorizerAI AIServiceProxy Expense<br>submitBalance(accountId, amount)<br>updateBalance(accountId, amount)<br>updateBalance(amount)<br>saveAccount(account)<br>confirmation<br>display updated balance<br>addCategory(name, balance)<br>createCategory(name, balance)<br>new Category(name, balance)<br>saveCategory(category)<br>confirmation<br>display new category<br>submitExpense(amount, description, date)<br>recordExpense(data)<br>categorizeExpense(expense)<br>generateResponse(request)<br>AIResponse<br>suggested Category<br>show suggested category<br>confirmCategory(category)<br>assignCategory(expense, category)<br>new Expense(amount, date, category)<br>saveExpense(expense)<br>confirmation<br>display updated expense<br><!-- End of picture text -->

Solid arrow = call.  Dashed arrow = return. 

Task 2.3 - Sequence Diagram 3 Savings Goal and Savings Recommendation (F05, F08) 



<!-- Start of picture text -->
User SavingsGoalGUI SavingsController SavingsGoalBuilder SavingsGoal Account DatabaseService FinancialDataFacade SavingsAI AIServiceProxy<br>submitGoal(name, target, timeframe, account)<br>createGoal(details)<br>setGoalDetails(name, target, timeframe)<br>setLinkedAccount(account)<br>build()<br>SavingsGoal<br>saveGoal(goal)<br>confirmation<br>calculateProgress()<br>getBalance()<br>balance<br>percentage<br>display progress bar<br>requestRecommendation()<br>generateSavingsRecommendation(goalId)<br>getFinancialData(userId)<br>load accounts, expenses, goals<br>records<br>FinancialData<br>analyzeSavingsGoal(goal, data)<br>calculateMonthlyTarget(goal, data)<br>generateResponse(request)<br>AIResponse<br>Recommendation<br>display amount and advice<br><!-- End of picture text -->

Solid arrow = call.  Dashed arrow = return. 

Task 2.3 - Sequence Diagram 4 AI Budget Recommendation (F07) 



<!-- Start of picture text -->
User BudgetGUI BudgetController FinancialDataFacade DatabaseService BudgetAI AIServiceProxy ChatGPTAdapter ChatGPT5API Budget<br>requestBudget(monthly or weekly)<br>generateBudget(period)<br>getFinancialData(userId)<br>load income, expenses, categories, goals<br>records<br>FinancialData<br>analyzeFinances(data)<br>calculateBaseline(data)<br>generateResponse(request)<br>checkAccess() and rate limit<br>generateResponse(request)<br>chatCompletion(payload)<br>raw response<br>adaptResponse(raw)<br>AIResponse<br>AIResponse<br>Recommendation<br>createBudget(recommendation)<br>saveBudget(budget)<br>display budget<br>suggested budget and category amounts<br><!-- End of picture text -->

Solid arrow = call.  Dashed arrow = return. 

# Task 2.3 - Sequence Diagram 5 Budget vs Actual Spending (F09) 



<!-- Start of picture text -->
User BudgetGUI BudgetController FinancialDataFacade DatabaseService Budget Expense<br>selectPeriod(period)<br>compareBudgetToActual(period)<br>getBudget(userId, period)<br>loadBudget(userId, period)<br>Budget<br>Budget<br>getExpenses(userId, period)<br>loadExpenses(userId, period)<br>Expense list<br>Expense list<br>calculateActual()<br>calculateDifference()<br>getLimit(category)<br>getCategory()<br>totals, percent over or under, category differences<br>display comparison<br><!-- End of picture text -->

Solid arrow = call.  Dashed arrow = return. 

Task 2.3 - Sequence Diagram 6 AI Chatbox with agent tool use (F06) 



<!-- Start of picture text -->
GetFinancialDataTo FinancialDataFacad<br>User ChatGUI AIController ChatAI AgentMemory AIServiceProxy ChatGPTAdapter ChatGPT5API ToolRegistry ol e<br>submitQuestion(text)<br>askQuestion(text)<br>processQuestion(text)<br>recall(userId)<br>past messages and preferences<br>run() and buildPrompt(text)<br>generateResponse(request)<br>checkAccess() and rate limit<br>generateResponse(request)<br>chatCompletion(payload)<br>raw response with tool call<br>AIResponse with tool call<br>AIResponse with tool call<br>execute(get_financial_data, args)<br>execute(args)<br>getFinancialData(userId)<br>FinancialData<br>ToolResult<br>ToolResult<br>add(observation)<br>generateResponse(request + ToolResult)<br>AIResponse with final answer<br>parseResult(response)<br>add(answer)<br>answer<br>displayResponse(answer)<br>answer shown<br><!-- End of picture text -->

Solid arrow = call.  Dashed arrow = return. 

