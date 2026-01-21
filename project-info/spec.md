# COMP2300 Coursework Specification 25/26

Lecturers: Dr Richard Gomer, Dr Haiming Liu, Dr Rafael Mestre, Dr Eike Schneiders, Dr Jian Shi, Dr Adriana Wilde, Dr Vahid Yazdanpanah

| Date | Author | Changes |
|----|----|----|
| 2025-09-28 | Richard Gomer | Interim draft |
| 2025-10-08 | Richard Gomer | Draft version 1; added new rubrics and D4-7 |
| 2025-10-17 | Richard Gomer | Minor changes to phrasing in some areas for clarity |
| 2025-12-03 | Richard Gomer | Updated D4 with some clarifying content. Fixed Github markdown link, pointed to Group Deliverable Report requirement from Process section and deliverable sections. |

## Welcome to the Software Design and Development Project!

On this module, you will work in groups to design and develop a novel software system that meets the needs of users. You will complete and submit a series of deliverables throughout the year, and you will be assessed on your team and individual contributions.

As well as the group work, you will individually submit a short individual reflective report about each deliverable. Those reports will be compiled and marked once per semester.

Alongside each deliverable, you will complete a peer-assessment of the other members of your group. Observations made by your group supervisor, and the contents of the individual reflective reports, and other information (for example the commit history from your project git repository) the peer-assessments will inform an individual "Peer-Review Informed Skills Assessment" (PRISM) mark.

> ⚠️ You should read this specification carefully, and refer to it throughout the year; individually and as a group. It contains important information about the module, including the assessment criteria. If you have any questions, please ask a member of the module team. The module *Discord channel* is a good place to ask questions, but you can also email the module team or speak to us in person.

## Contents

- [Welcome to the Software Design and Development Project!](#section-0-Welcome%20to%20the%20Software%20Design%20and%20Development%20Project!)
- [Assessment Components](#section-1-Assessment%20Components)
- [Aims and Learning Outcomes](#section-1-Aims%20and%20Learning%20Outcomes)
  - [A Knowledge and Understanding](#section-1-A%20Knowledge%20and%20Understanding)
  - [B Subject Specific Intellectual and Research Skills](#section-1-B%20Subject%20Specific%20Intellectual%20and%20Research%20Skills)
  - [C Transferable and Generic Skills](#section-1-C%20Transferable%20and%20Generic%20Skills)
- [Use of GenAI](#section-2-Use%20of%20GenAI)
- [Group Allocation](#section-3-Group%20Allocation)
- [Design Challenges](#section-4-Design%20Challenges)
  - [1. Digital Sexual Health](#section-4-1.%20Digital%20Sexual%20Health)
  - [2. Effective Home Energy Use](#section-4-2.%20Effective%20Home%20Energy%20Use)
- [Tools and Technology](#section-5-Tools%20and%20Technology)
- [Project Management](#section-6-Project%20Management)
- [Group Supervisor](#section-7-Group%20Supervisor)
- [The Process](#section-8-The%20Process)
  - [D1: Understanding the Problem Space](#section-9-D1:%20Understanding%20the%20Problem%20Space)
  - [D2: Conceptual Design](#section-10-D2:%20Conceptual%20Design)
  - [D3: Prototype Design & Evaluation Plan](#section-11-D3:%20Prototype%20Design%20&%20Evaluation%20Plan)
  - [D4: Technical and Project Planning](#section-12-D4:%20Technical%20and%20Project%20Planning)
  - [D5: Increment 1](#section-12-D5:%20Increment%201)
  - [D6: Increment 2](#section-12-D6:%20Increment%202)
  - [D7: Increment 3](#section-12-D7:%20Increment%203)
  - [D8: Evaluation Report](#section-13-D8:%20Evaluation%20Report)
- [Group Meetings](#section-14-Group%20Meetings)
  - [Meeting Basics](#section-14-Meeting%20Basics)
  - [Submitting your meeting records](#section-14-Submitting%20your%20meeting%20records)
- [Group Deliverable Reports](#section-15-Group%20Deliverable%20Reports)
- [PRISM (40%)](#section-16-PRISM%20(40%))
  - [Minimum Expectations](#section-16-Minimum%20Expectations)
  - [Peer Review Process](#section-16-Peer%20Review%20Process)
  - [PRISM Marking Rubric](#section-16-PRISM%20Marking%20Rubric)
- [The Individual Reflective Reports (10%)](#section-17-The%20Individual%20Reflective%20Reports%20(10%))
  - [What to Submit](#section-17-What%20to%20Submit)
  - [Individual Reflective Report Marking Criteria](#section-17-Individual%20Reflective%20Report%20Marking%20Criteria)
- [Presentation Guidelines and Referencing](#section-18-Presentation%20Guidelines%20and%20Referencing)
  - [PDF Formatting](#section-18-PDF%20Formatting)
  - [PDF Title Pages](#section-18-PDF%20Title%20Pages)
  - [Markdown](#section-18-Markdown)
  - [Citations](#section-18-Citations)
  - [Word Limits](#section-18-Word%20Limits)

## Assessment Components

Marks for this module are formed from three formal components.

| Component | Weighting | Learning Outcomes | Deadlines |
|----|----|----|----|
| Group Project Deliverables | 50% | A2, B1, B2, C1, C2, C3, C4 | Throughout the year |
| Individual Reflective Reports | 10% | A1, C1, C2, C5 | Throughout the year |
| Peer Review Informed Skill Assessment | 40% | A1, A2, A3, A4, C1, C2 | Weekly peer-reviews |

## Aims and Learning Outcomes

### A Knowledge and Understanding

**Having successfully completed the module, you will be able to demonstrate knowledge and understanding of:**

A1. The multidisciplinary nature of the design of software systems, and the range of contributory disciplines (software engineering, human factors, cognitive psychology, graphics design, ethics, etc.).

A2. User-centred and participatory design processes.

A3. The main concepts that underpin the design of user interfaces.

A4. Agile methodologies for software project management.

### B Subject Specific Intellectual and Research Skills

**Having successfully completed this module, you will be able to:**

B1. Specify, design, develop, test and evaluate a prototype interactive software system.

B2. Apply appropriate software engineering techniques and tools.

### C Transferable and Generic Skills

**Having successfully completed this module, you will be able to:**

C1. Work effectively as part of a team, recognising the different roles within the team, and reflecting on both your own and the team's performance.

C2. Manage a project effectively.

C3. Solve complex problems in creative and innovative ways.

C4. Identify, evaluate and mitigate risks.

C5. Reflect on your self-learning.

## Use of GenAI

All assessments on COMP2300 are Tier 2 assessments.

You may use Generative AI to help you complete the assignment, should you wish. However, you are solely responsible for the quality and correctness of the work that you submit.

If Generative AI is used to produce any of the material that you hand in, you must submit an additional file `genai.txt` , which describes how it was used; including the names and versions of the tool, and the purposes for which you used it. Failure to disclose the use of GenAI is a breach of the Academic Responsibility and Conduct Regulations.

> ⚠️ You should be aware that Generative AI tools produce low-quality, imprecise, and superficial output, which does not meet our expectations for degree-level work. To obtain a high mark, you must check, edit and improve any GenAI output that you use based on your own knowledge, understanding, and insight.

## Group Allocation

You will be assigned to a group of students by week 3. We will take a range of factors into account when assigning groups, based on our experience and the research that has been undertaken into running group assignments effectively.

Groups will have four or five members. There is no particular advantage to being in a team of four or five, and group size will not be reflected in the marking. In larger teams, communication and coordination overheads are greater, but there are more people to do the work.

You should aim to establish a collaborative and supportive team culture from the outset. This includes setting clear expectations for communication and collaboration, as well as being open to feedback and discussion. Groups will often contain members with different skillsets, backgrounds, and experiences. You should consider how to make everyone feel included and valued within the group, and take strengths into account when assigning roles and responsibilities.

You **must** contribute to the group work and engage with your teammates. This means attending meetings, participating in discussions, and completing your assigned tasks on time. If you are struggling with your workload or have any concerns about the group dynamics, you should raise these issues with your group members or seek support from the group supervisor, module team, or personal academic tutor. Even with a very high mark on the group component, it is possible to fail the module if you do not participate effectively.

## Design Challenges

You must pick one of the two design challenges for the project. In both challenges, you have a client who has provided some context and background information, and who would like you to design an interactive tool of some kind to help address the challenge. In both cases, your client is looking for new ideas and is prepared to give you a high degree of freedom in terms of the design of the tool. You must use your creativity and judgement to design something that you think would be useful and usable.

During your first group meeting, you should discuss the two challenges and decide which one you would like to work on. You should record your choice in your meeting notes.

### 1. Digital Sexual Health

Sexual Health Services are an important part of protecting individual and wider public health. However, rates of sexually transmitted infections have been rising in the UK, putting more pressure on services and the councils and integrated care systems that fund them.

Your client is a sexual health service provider, who would like you to help them improve the experience of their users, and to help them manage demand for their services. They have undertaken interviews with some of their service users and staff to help you understand the context and the challenges that they face.

Design an interactive tool of some kind that would help individuals and/or professionals (such as a counsellor or advisor) support people to look after their sexual health. You could consider different interactions that people have with sexual health services, potentially including one or more of:

- providing advice or information
- making and planning appointments
- arranging STI screening or vaccination
- accessing test results
- advising on medication
- arranging routine care

Papers to get you started:

- Bennett, C. et al. 2023. The barriers and facilitators to young people’s engagement with bidirectional digital sexual health interventions: a mixed methods systematic review. BMC Digital Health. 1, 1 (Aug. 2023), 30. <https://doi.org/10.1186/s44247-023-00030-3>
- Dewan, U. et al. 2024. Teen Reproductive Health Information Seeking and Sharing Post-Roe. CHI2024. <https://dl.acm.org/doi/10.1145/3613904.3641934>
- McKellar, K. et al. 2017. Exploring the Preferences of Female Teenagers when Seeking Sexual Health Information using Websites and Apps. Proc 2017 International Conference on Digital Health. <https://dl.acm.org/doi/10.1145/3079452.3079497>
- Wood, M. et al. 2018. “Protection on that Erection?”: Discourses of Accountability & Compromising Participation in Digital Sexual Health. CHI2018. <https://dl.acm.org/doi/10.1145/3173574.3174238>
- Yuan, C.W. (Tina) et al. 2023. Understanding and Designing Multi-level Preventive Medication Support Against HIV for Men who Have Sex with Men in Taiwan. CSCW2023. <https://doi.org/10.1145/3610101>
- Nadarzynski et al. 2024, Achieving health equity through conversational AI: A roadmap for design and implementation of inclusive chatbots in healthcare, <https://journals.plos.org/digitalhealth/article?id=10.1371/journal.pdig.0000492>

Other resources:

- England's sexual health services 'at breaking point', 20th Jan 2024, BBC News, <https://www.bbc.co.uk/news/health-68016937>
- Hampshire Sexual Health Service, <https://www.letstalkaboutit.nhs.uk/>
- SH:24 – An online sexual health service, <https://sh24.org.uk/>

### 2. Effective Home Energy Use

A range of energy technologies like solar panels, battery storage, and electric vehicles are now available, and are becoming commonplace in people’s homes. However, making best use of energy using these technologies is non-trivial.

Your client is an energy provider, who would like you to help them design a tool that helps people to make best use of energy in their homes. They have undertaken interviews with some of their customers to help you understand the context and the challenges that they face.

Design an interactive tool of some kind that helps people to make use of energy in their homes, using currently available off-the-shelf hardware. You could consider designing for households that include one or more of:

- Domestic photovoltaic panels
- Battery storage
- Electric vehicles (including those with “vehicle to grid” capability)
- Electric space and/or water heating
- Time-of-use energy tariffs (for example, Agile Octopus)
- Energy export tariffs (for example, Outgoing Octopus)

Papers to get you started:

- Michael K. Svangren et al. 2018. Driving on sunshine: aligning electric vehicle charging and household electricity production. NordCHI 2018. <https://dl.acm.org/doi/abs/10.1145/3240167.3240179>
- Jesper Kjeldskov et al. 2015. Eco-Forecasting for Domestic Electricity Use. CHI '15. <https://doi.org/10.1145/2702123.2702318>
- Filipe Quintal et al. 2019. MyTukxi: Low Cost Smart Charging for Small Scale EVs. CHI EA '19 <https://doi.org/10.1145/3290607.3312874>
- Michal Luria, Guy Hoffman, and Oren Zuckerman. 2017. Comparing Social Robot, Screen and Voice Interfaces for Smart-Home Control. CHI '17. <https://doi.org/10.1145/3025453.3025786>
- Brewer, R.S. et al. 2015. Tough Shift: Exploring the Complexities of Shifting Residential Electricity Use Through a Casual Mobile Game. Proc. 2015 Annual Symposium on Computer-Human Interaction in Play. <https://dl.acm.org/doi/10.1145/2793107.2793108>
- Chowdhury, N. and Moore, J. 2018. Attention Management for Improved Renewable Energy Usage at Households Using IoT-enabled Ambient Displays. Proc. 2018 ACM International Joint Conference on Pervasive and Ubiquitous Computing and Wearable Computers <https://dl.acm.org/doi/10.1145/3267305.3274110>

Other resources:

- Overview of Agile Octopus (a time-of-use electricity tariff) <https://octopus.energy/agile/>

## Tools and Technology

During the project, you will need to make use of a range of different tools and technologies for data analysis, prototyping, programming, testing, evaluation, version control and project management.

For the most part, you can (and must) choose which tools and technologies you would like to use. However, there are some basic requirements that you must follow.

#### groups.ecs

You **must** use [groups.ecs.soton.ac.uk](https://groups.ecs.soton.ac.uk/) to record your weekly meeting notes and actions. You may use other project management tools in addition to groups.ecs if you wish to do so.

#### GitLab

You **must** use UoS [GitLab](https://git.soton.ac.uk/) to store your source code during the build of your project. We will consider commits made to your gitlab repository when assessing individual contributions to the project.

#### Java and JavaFX

You **must** implement the project using Java and JavaFX.

## Project Management

During the module, you will learn the basis of project management using SCRUM and Agile techniques. You will be expected to apply these techniques to manage your project effectively.

As a team, you should agree on how you will manage your project, including how you will manage risks. You should document your approach in the deliverables, as described in the deliverable template.

Keep records of your project management activities, including things like risk logs, user stories, burndown charts, and include them in your deliverable reports. You'll only get credit for project management activities that you document.

> ⚠️ Non-contribution by group members (dues to illness, absence, laziness or any other reason) is a normal project risk, which you must plan for. On this module, the absence of group members is not grounds for a Special Consideration request by the remainder of the group. You should plan for this risk and mitigate it appropriately.

## Group Supervisor

You will be assigned a group supervisor. The supervisor has two main jobs:

1.  To act as your client. You can have conversations with your supervisor about requirements, and they can tell you what they would like from the system that you are building. But remember, they are not the only stakeholder!
2.  To observe your meetings. The supervisor will observe your meetings, and use their observations to inform your individual PRISM mark.

Your supervisor will play a role in assessing your work, but for information about our expectations for each deliverable you should contact the **deliverable champion** .

## The Process

During the project, you will prepare and submit **eight** deliverables as a group.

The deliverables include design and software engineering artifacts and accompanying documentation, and reflect the skills and theory that are taught during the module.

> Along with the specific items mentioned for each deliverable, you must submit a deliverable report that covers general process and project management information. Please see the section on \*\* Group Deliverable Reports\*\* for more information.

A deliverable is due every TWO or THREE teaching weeks (excluding holidays and exam periods), and specific deadlines are included in the overall course schedule.

### D1: Understanding the Problem Space

**Deliverable Champion: Richard Gomer**

#### Literature Review

You should conduct a review of appropriate peer-reviewed literature, to help you better understand the challenge area, and to identify design solutions that have already been tried. Some papers are suggested in the design challenge descriptions, but you should search for additional relevant literature.

To find research into other interactive tools related to the challenge, you may find it helpful to try searching the ACM Digital Library at <https://dl.acm.org/> as well as other literature databases that are available via the Library.

You should summarise the relevant literature that you find, and identify some specific challenges that interactive technology could help to address. As well as identifying what is currently known (and what approaches have already been tried), you should identify “gaps” in the current literature - i.e. things that it would be helpful for you to know, but which the literature does not contain sufficient information about.

You must cite properly - see the section about citations later in this specification.

**Your literature review should be no more than two sides of A4.**

#### Interview Analysis

Analyse a set of provided interview transcripts, and produce a thematic analysis of the data. **Transcripts will be made available at the start of Week 4.**

The transcripts cover relevant topics with a range of potential users. You should conduct a thematic analysis of the interviews to identify important themes in the data.

You should code the transcripts using first-pass coding, and then identify themes that help you to understand the design space.

You should produce a description of themes that you have found, in enough detail that somebody could understand your findings without having to read the interviews themselves. You should include some example quotes with each theme.

You are encouraged to identify some “latent” themes. Latent themes go beyond the semantic content of the data, and start to identify or examine the underlying ideas, assumptions, and conceptualisations that have shaped the semantic content of the data.

#### Job to be Done

Identify a "job to be done", based on your thematic analysis. The job to be done is a concise statement of task, job or problem that users are trying to accomplish, and which your system will address.

The Job to be Done should be narrower than the original design challenge; i.e. it should identify a specific problem that people have within the wider design space. You should refer to evidence (e.g. themes or quotes) from the interviews, and the literature, to justify it and demonstrate that it has been informed by the evidence available.

#### What to Submit

Please submit a single PDF file, containing your literature review, thematic analysis, and description of the job to be done. You should include any tables or figures that you have produced, and you should include example quotes from the interviews to illustrate your themes.

> Don't forget to include the usual **[Group Deliverable Report](#section-15-Group%20Deliverable%20Reports)** as part of your submission.

#### D1 Marking Rubric

|  | 0-40 | 50 | 60 | 70 | 80+ |
|----|----|----|----|----|----|
| Literature Review | No literature review is presented, or the review is irrelevant or incoherent. | A basic literature review is presented, but it is superficial or unclear. It may not clearly relate to design. | A satisfactory literature review is presented, with clear identification and explanation of relevant work. Some connections to design are made. | A good literature review is presented, with well-identified and clearly explained relevant work. Useful connections to design are made. | An excellent literature review is presented, with insightful identification and thorough explanation of relevant work. Very useful connections to design are made. |
| Thematic Analysis | No thematic analysis is presented, or the analysis is irrelevant or incoherent. | A basic thematic analysis is presented, but it lacks depth or clarity. Themes may be categories, rather than broad patterns. | A satisfactory thematic analysis is presented, with clear identification and explanation of themes. Some themes are useful and explain important patterns. | A good thematic analysis is presented, with well-identified and clearly explained themes. Most themes are useful and explain important patterns. | An excellent thematic analysis is presented, with insightful identification and thorough explanation of themes. Virtually all themes are useful and explain important patterns. |
| Job to be done | No problem statement is presented, or the statement is irrelevant or incoherent. | A basic problem statement is presented, but it lacks clarity or justification. It may not be clearly linked to the thematic analysis. | A satisfactory problem statement is presented, with some clarity and justification. It is generally linked to the thematic analysis. | A good problem statement is presented, with clear articulation and strong justification. It is well linked to the thematic analysis. | An excellent problem statement is presented, with precise articulation and compelling justification. It is very well linked to the thematic analysis. |

### D2: Conceptual Design

**Deliverable Champion: Eike Schneiders**

#### Personas and Scenarios

Produce a set of user personas and scenarios, that reflect the needs and goals of potential users of your system.

You should produce at least three personas, and at least three scenarios. Each persona should be distinct, and represent a different stakeholder.

Your personas should include any features that you think are relevant to understanding their needs and goals, and how they might interact with your system. This might include demographic information, but should also include information about their attitudes, behaviours, motivations, and frustrations that are relevant to the design challenge.

Each scenario should describe a specific situation in which the system is used (directly or indirectly) to help the persona achieve a goal.

#### Design Concepts

You should ideate a wide range of possible design responses to the problem statement, and collect evidence of the ideation process that you used, for instance in the form of a description and photographs or screenshots. You should then identify THREE distinct concepts, which you think show most potential for refinement into an initial prototype.

You should describe each concept using sketches, diagrams and text as necessary, and clearly explain how each design concept could address your stated challenge.

#### What to Submit

Please submit a single PDF file, containing your personas and scenarios, and your three design concepts. You should include any sketches or diagrams that you have produced, and photographs or screenshots of your ideation process.

> Don't forget to include the usual **[Group Deliverable Report](#section-15-Group%20Deliverable%20Reports)** as part of your submission.

#### D2 Marking Rubric

|  | 0-40 | 50 | 60 | 70 | 80+ |
|----|----|----|----|----|----|
| Personas & Scenarios | No personas or scenarios are presented, or they are irrelevant or incoherent. | Basic personas and scenarios are presented, but they lack detail or relevance to the design process or challenge. They may not clearly reflect user needs and goals. | Satisfactory personas and scenarios are presented, with some detail and relevance to the design process or challenge. They generally reflect user needs and goals. | Good personas and scenarios are presented, with clear detail and strong relevance to the design process or challenge. They effectively reflect user needs and goals. | Excellent personas and scenarios are presented, with rich detail and high relevance to the design process or challenge. They thoroughly reflect user needs and goals. |
| Design Ideas | No design ideas are presented, or the ideas are irrelevant or incoherent. | Basic design ideas are presented, but they lack creativity or clarity. They may not clearly address the problem statement. | Satisfactory design ideas are presented, with some creativity and clarity. They generally address the problem statement. Some ideas are not conceptually distinct. | Good design ideas are presented clearly. Three distinct concepts that each address the problem statement. | Excellent design ideas are clearly presented and well justified. Three distinct concepts that each address the problem statement that each address the problem statement. |
| Description & Justification | No descriptions or justifications are presented, or they are irrelevant or incoherent. | Basic descriptions and justifications are presented, but they lack detail or relevance. They may not clearly explain how the ideas address the problem statement. | Satisfactory descriptions and justifications are presented, with some detail and relevance. They generally explain how the ideas address the problem statement. | Good descriptions and justifications are presented, with clear detail and strong relevance. They effectively explain how the ideas address the problem statement. | Excellent descriptions and justifications are presented, with rich detail and high relevance. They thoroughly explain how the ideas address the problem statement. |

### D3: Prototype Design & Evaluation Plan

**Deliverable Champion: Rafael Mestre**

#### Coarse User Stories

Define a set of coarse user stories that describe the key functionality of your proposed system. You should identify core user stories for each stakeholder, and you should prioritise them using MoSCoW prioritisation (i.e. Must have, Should have, Could have, Won't have).

At this stage, your user stories should be coarse-grained, and suitable to support your low-fidelity prototyping. You will refine and expand your user stories in later deliverables.

#### Lo-Fi Prototype

Decide which of your concepts you would like to develop, and create a low-fidelity prototype that shows how your design concept could work. The prototype may be created using paper prototyping, cardboard, physical materials or include material developed with any appropriate app / web page prototyping software: Moqup, Balsamiq, Figma, etc. You are not expected to do any programming, and you will lose marks if the fidelity of your prototype is inappropriate.

Your low-fidelity prototype will, by definition, not be a polished design; but it should not have obvious conceptual or usability problems. You should give appropriate consideration to how your prototype could be made accessible.

Your prototype should be novel (i.e. not substantially the same as an existing product) and should be technically feasible to deliver by a small software/hardware company incorporating technology which exists now (i.e. no time travel or “proposed future technology”).

You must not rely on speculative “machine learning”. Your prototype should do more than just log data and display it to users.

In addition to the prototype itself, you should prepare a description of your prototype using text, sketches and diagrams to help others understand what you are proposing.

#### Evaluation Plan

You should prepare a plan for evaluating your final, high-fidelity, prototype at the end of the project. The evaluation plan should include a description of the evaluation method that you will use, and the participants that you will recruit. You should include the evaluation protocol including the tasks that participants will complete and the questions that they will be asked.

You should describe the key criteria that will allow you to judge whether the protoype is successful, and you should describe how you will analyse the data that you collect.

Your evaluation must adhere to the following restrictions:

- Participants in your evaluation must be from another COMP2300 group.
- You must recruit the participants yourself (by finding another group and asking them to take part).
- You must use a user-centred evaluation method (e.g. usability testing, think-aloud protocol, cognitive walkthrough, etc.)
- The evaluation must take place in a lab setting (i.e. not remotely, and not in the wild; although this needn't be an actual lab - just a quiet room where you can control the environment).
- You must not collect or try to elicit any sensitive personal data <sup>1</sup> (e.g. health data, sexual health data, political or religious beliefs, etc.) from participants.

> ⚠️ A heuristic evaluation is NOT a user-centred evaluation method, and is not acceptable (by itself) for your final evaluation. However, you may choose to conduct a heuristic evaluation of your user interface in addition to another form of evaluation, if you wish to do so.

> <sup>1</sup> As defined in the UK General Data Protection Regulation (UK GDPR), see <https://ico.org.uk/for-organisations/guide-to-data-protection/guide-to-the-general-data-protection-regulation-gdpr/special-category-data/>

#### What to Submit

Please submit a single PDF file, containing your deliverable report, pictures/sketches/description of your prototype, and your evaluation plan.

> Don't forget to include the usual **[Group Deliverable Report](#section-15-Group%20Deliverable%20Reports)** as part of your submission.

#### D3 Marking Rubric

|  | 0-40 | 50 | 60 | 70 | 80+ |
|----|----|----|----|----|----|
| User Stories | No user stories are presented, or the stories are irrelevant or incoherent. | Basic user stories are presented, but they lack detail or clarity. They may not clearly reflect key functionality or be prioritised. | Satisfactory user stories are presented, with some detail and clarity. They generally reflect key functionality and are prioritised. | Good user stories are presented, with clear detail and clear links to the personas and earlier analysis. They effectively reflect key functionality and are well prioritised. | Excellent user stories are presented, with rich detail and good links to the personas and earlier analysis. They thoroughly reflect key functionality and are expertly prioritised. |
| Lo-Fi Prototype | No prototype is presented, or the prototype is irrelevant or incoherent or of an inappropriate fidelity. | A basic prototype is presented, but it lacks detail or clarity. It may not clearly reflect the design concept, or have aspects that are an inappropriate fidelity. | A satisfactory prototype is presented, with some detail and clarity. It generally reflects the design concept. | A good prototype is presented, with clear detail. It effectively reflects the design concept. | An excellent prototype is presented, with rich detail. It thoroughly reflects the design concept. |
| Evaluation Plan | No evaluation plan is presented, or the plan is irrelevant or incoherent. | A basic evaluation plan is presented, but it lacks detail or clarity. It may not clearly describe a user-centred evaluation. | A satisfactory evaluation plan is presented, with some detail. It describes a generally sound user-centred evaluation. | A good evaluation plan is presented, with clear detail and strong relevance. It effectively reflects a user-centred evaluation. | An excellent evaluation plan is presented, with rich detail and high relevance. It thoroughly reflects a user-centred evaluation. |

### D4: Technical and Project Planning

**Deliverable Champion: Jian Shi**

#### Requirements Planning

You must plan requirements for the system, based on your analysis and design in previous deliverables. The user stories that you put forward in this deliverable should be more fine-grained than those in Deliverable 2, and should be prioritised and estimated. You should include a MoSCoW prioritisation for your requirements, and you should estimate the effort required to implement each requirement. You should ensure that your requirements follow the INVEST principles and are presented in an appropriate format.

#### Technical Design

You should produce and justify a technical design for your system, including appropriate UML diagrams and any other relevant design artifacts. You should justify your design decisions, and explain how your design meets the requirements that you have planned.

<div class="update">

You should organise your existing sketches and designs so that they are linked to the specific user stories that they support. Artefacts may be new or revised since previous deliverables, reflecting conversations with your client and within your team.

For up to five of the most important user stories, you should provide an updated scenario (of a paragraph) that describes how a user would interact with the system to achieve a goal. These scenarios should be written to support testing of the story later.

</div>

#### Project Planning

You should produce a project plan for the implementation of your system, including an initial allocation of user stories to sprints and a detailed plan for the first sprint. Prepare burndown charts and other project management artifacts as appropriate. You should ensure that your project plan is realistic and achievable, and that it takes into account any risks that you have identified. Sensible deliverables should be chosen for each sprint, so that value is delivered at the end of each sprint.

#### Project Setup

You should set up your project management and development environment, including your GitLab repository, build tools, and any other necessary tools to support Agile SCRUM. You should ensure that your team members have access to the necessary resources, and that you have a clear plan for how you will manage your project. You should identify and analyse the main risks to your project, and produce a risk management plan that includes mitigation strategies for each risk.

#### What to Submit

Please submit a <span class="update"> **PDF file, word file, or presentation deck** </span> , containing your requirements plan, technical design, project plan, and evidence of your project setup. You should include any UML diagrams or other design artifacts that you have produced, and any project management artifacts that you have created.

<span class="update">The substantive content of this report (excluding title pages, and the group deliverable report) should be no more than 15 slides or 10 pages.</span>

> Don't forget to include the usual **[Group Deliverable Report](#section-15-Group%20Deliverable%20Reports)** as part of your submission.

#### D4 Marking Rubric

|  | 0-40 | 50 | 60 | 70 | 80+ |
|----|----|----|----|----|----|
| Requirements Planning | No user stories are presented, or they are irrelevant/incoherent. No evidence of INVEST, backlog prioritisation, or testing plan. | Basic user stories are presented in correct format, but lack detail or coverage. INVEST characteristics are weak. Limited or missing plan for testing. Backlog prioritisation is incomplete. | Satisfactory user stories are presented, with some detail and mostly correct format. Some INVEST characteristics are met. A testing plan is present but lacks detail. Backlog includes some prioritisation but not fully consistent. | Good user stories are presented, with clear detail and correct format. They broadly meet INVEST characteristics. A clear plan exists for testing user stories. Product backlog is well prioritised using MoSCoW. | Excellent user stories are presented, with rich detail and coverage of key requirements. They fully satisfy INVEST characteristics. Each story has a well-thought-out testing plan. The product backlog shows excellent and consistent MoSCoW prioritisation. |
| Design | No design decisions or artifacts are presented, or they are irrelevant/incoherent. No architecture is chosen. | Basic design decisions are made but lack clarity or justification. Architecture is simplistic or unsuitable. Few or irrelevant design artifacts are used. | Satisfactory design decisions are made with some justification. Architecture is present and workable but may not support maintainability strongly. Some key artifacts (e.g., UML diagrams) are included but limited in quality. | Good design decisions are made and justified, with a suitable architecture that supports maintainability. A variety of relevant design artifacts are presented and used effectively to support decisions. | Excellent design decisions are made and thoroughly justified. The architecture is well chosen and facilitates long-term maintainability. Rich and appropriate design artifacts (UML, sketches, scenarios, etc.) are presented and clearly linked to the decisions. |
| Project Planning | No increment or sprint plan is presented, or plans are incoherent. No burndown chart or backlog is included. | Basic increment and sprint plans are presented, but they lack detail or feasibility. Deliverables are vague. A burndown chart exists but is incorrectly formatted. | Satisfactory increment and sprint plans are presented, with some clear deliverables and prioritisation. The sprint backlog is present. The burndown chart is in place but lacks consistency or clarity. | Good increment and sprint plans are presented, with sensible deliverables and clear prioritisation of user stories. The sprint backlog is complete. The burndown chart is correctly formatted and informative. | Excellent increment and sprint plans are presented, with well-chosen deliverables that maximise value and feasibility. Plans clearly reflect prioritisation. A detailed and complete sprint backlog is provided. The burndown chart is in the correct format and highly effective. |
| Project Set-up | No evidence of project infrastructure, tools, or risk analysis. | Basic project infrastructure and agile tools are presented, but they may not be functional. Risks are identified but mitigation strategies are weak or missing. | Satisfactory project infrastructure is set up and agile tools are in use. Main risks are identified with some sensible mitigation, but not comprehensive. | Good project infrastructure is established, and agile tools are effectively used. Risks are clearly identified with strong mitigation strategies. | Excellent project infrastructure is set up, using appropriate agile tools. Risks are comprehensively identified with well-justified mitigation strategies. The set-up shows strong readiness for ongoing development. |

### D5: Increment 1

**Deliverable Champion: Jian Shi**

#### Design and Implementation

You should develop and build the first increment of your system, implementing the user stories that you have planned for the first sprint. You should ensure that your implementation follows best practices for software development, including code quality, documentation, and testing.

The application must compile and run without errors, and must meet the acceptance criteria that you have defined for the user stories that you are implementing. The application should follow the designs that you have produced, and should be user-friendly and accessible.

You should make sensible design decisions to support development and maintainability. You should document your design decisions, using appropriate UML diagrams and other design artifacts as necessary. You should incorporate any feedback from your previous deliverables.

#### Testing

You should undertake some testing using appropriate methods to test and verify the correctness of the application. Where appropriate, tests should be based on relevant design artefacts.

#### Planning and Management

You should manage your project effectively, following the project plan that you produced in Deliverable 4. You should keep records of your project management activities, including things like user stories and a burndown chart, and include them in your deliverable report. Work should progress according to the sprint plan, or the plan should be revised. You should prepare a detailed plan for Sprint 2.

#### What to Submit

Please submit a <span class="update"> **PDF file, word file, or presentation deck** </span> , containing your project documentation. You should include any new or updated UML diagrams or other design artifacts that you have produced, and any project management artifacts that you have created, and evidence of your testing. You should include a link to your GitLab repository.

Also submit a **zip or tar archive** containing your Java application and any jar files or other dependencies that are necessary to run it. The application must compile and run without errors.

> Don't forget to include the usual **[Group Deliverable Report](#section-15-Group%20Deliverable%20Reports)** as part of your submission.

#### D5 Marking Rubric

|  | 0-40 | 50 | 60 | 70 | 80+ |
|----|----|----|----|----|----|
| Application | The application does not compile or run. No useful functionality is delivered. Outputs are missing or incorrect. User controls are absent or unusable. | The application compiles and runs, but only basic functionality is delivered. Outputs are partially correct or unclear to the customer. User controls are minimal and not intuitive. | The application compiles and runs with satisfactory functionality. Outputs are generally correct and somewhat clear. User controls are present but could be improved for intuitiveness. Some value is delivered to the customer. | The application compiles and runs well, delivering useful functionality. Outputs are correct and clearly displayed. User controls are appropriate and intuitive. The increment provides clear value to the customer. | The application compiles and runs flawlessly, delivering excellent functionality and strong value to the customer. Outputs are correct, clear, and well formatted. User controls are highly intuitive and enhance usability. |
| Design | No sensible design decisions are evident. No architecture or artifacts are presented. Feedback from previous deliverables is ignored. | Basic design decisions are made but lack clarity or justification. Architecture is simplistic or unsuitable. Few design artifacts are presented. Feedback from the previous deliverable is only partially addressed. | Satisfactory design decisions are made, with some justification. A workable architecture is presented. Some suitable design artifacts are included. Feedback from the previous deliverable is considered but not fully integrated. | Good design decisions are made and justified. The architecture supports maintainability. Relevant design artifacts are presented and support decisions. Prioritisation is sensible, and feedback from the previous deliverable is incorporated. | Excellent design decisions are well justified. The architecture is robust and facilitates maintainability. Rich and appropriate design artifacts (UML, sketches, scenarios, etc.) are used effectively. Prioritisation ensures maximum value. Feedback from previous deliverables is fully addressed. |
| Testing | No testing evidence is presented. No steps are taken to verify correctness. | Basic testing steps are taken, but they are limited or superficial. Some outputs are tested, but evidence is weak. | Satisfactory testing is carried out, with some evidence of test outputs. Some tests are linked to design artifacts. Coverage is partial. | Good testing is carried out systematically. Clear evidence of test outputs is provided. Tests are linked to design artifacts and effectively verify correctness. | Excellent and comprehensive testing is carried out. Strong evidence of test outputs is presented. Tests are clearly linked to design artifacts and thoroughly verify correctness. |
| Planning | No burndown chart or sprint plan is presented. No evidence of progress tracking or reprioritisation. | A basic burndown chart is presented, but it may be incorrect or incomplete. Sprint planning for the next increment is weak. Progress tracking is minimal. | A satisfactory burndown chart is presented with some accuracy. Sprint planning for the next increment exists but lacks detail. Some reprioritisation of work is shown. | A good burndown chart is presented, showing clear progress. Sprint planning for the next increment is appropriate. Progress is tracked, and reprioritisation is managed effectively. | An excellent burndown chart is presented in the correct format, with detailed and accurate tracking of progress. Sprint planning for the next increment is clear, detailed, and realistic. Reprioritisation is well justified. |

### D6: Increment 2

**Deliverable Champion: Jian Shi**

Increment 2 should proceed in the same way as Increment 1, with the implementation of the user stories that you have planned for the second sprint. You should continue to follow best practices for software development, including code quality, documentation, and testing.

In addition to the criteria for Increment 1, you should develop your practices as follows.

#### Testing

- Ensure adequate test coverage of your code, using boundary and partition testing.
- Test your application against well-defined acceptance criteria for the user stories that you are implementing.
- Test your application against scenarios.

#### Planning and Management

- Maintain a summary of each team member's tasks and hours worked.
- Provide a clear definition of done and follow it correctly.

#### What to Submit

Please submit a <span class="update"> **PDF file, word file, or presentation deck** </span> , containing your project documentation. You should include any new or updated UML diagrams or other design artifacts that you have produced, and any project management artifacts that you have created, and evidence of your testing. You should include a link to your GitLab repository.

Also submit a **zip or tar archive** containing your Java application and any jar files or other dependencies that are necessary to run it. The application must compile and run without errors.

> Don't forget to include the usual **[Group Deliverable Report](#section-15-Group%20Deliverable%20Reports)** as part of your submission.

#### D6 Marking Rubric

|  | 0-40 | 50 | 60 | 70 | 80+ |
|----|----|----|----|----|----|
| Application | The application does not compile or run. No useful functionality is delivered. Outputs are missing or incorrect. User controls are absent or unusable. | The application compiles and runs, but only basic functionality is delivered. Outputs are partially correct or unclear. User controls are minimal and not intuitive. Limited value is provided to the customer. | The application compiles and runs with satisfactory functionality. Outputs are generally correct and somewhat clear. User controls are present but could be improved. Some value is delivered to the customer. | The application compiles and runs well, delivering planned functionality as specified. Outputs are correct and clearly displayed. User controls are appropriate and intuitive. The increment provides clear value to the customer. | The application compiles and runs flawlessly, delivering excellent functionality and strong value to the customer. Outputs are correct, clear, and well formatted. User controls are highly intuitive and enhance usability. |
| Design | No sensible design decisions are evident. No architecture or artifacts are presented. Feedback from previous deliverables is ignored. | Basic design decisions are made but lack clarity or justification. Architecture is simplistic or unsuitable. Few design artifacts are presented. Feedback is only partially addressed. | Satisfactory design decisions are made, with some justification. A workable architecture is presented. Some suitable artifacts are included. Feedback is considered but not fully integrated. | Good design decisions are made and justified. The architecture supports maintainability. Relevant design artifacts are presented and support decisions. Prioritisation is sensible, and feedback from the previous deliverable is incorporated. | Excellent design decisions are well justified. The architecture is robust and facilitates maintainability. Rich and appropriate artifacts (UML, sketches, scenarios, etc.) are used effectively. Prioritisation ensures maximum value. Feedback from previous deliverables is fully addressed. |
| Testing | No testing is carried out. No unit tests, coverage, or acceptance criteria are provided. | Basic testing steps are taken, but they are limited or superficial. Few or weak unit tests exist. Little evidence of coverage or acceptance criteria. | Satisfactory testing is carried out, with some unit tests and partial coverage. Some boundary/partition testing is attempted. Regression testing and acceptance criteria are limited. | Good systematic testing is performed, with clear unit tests for key components. Coverage is appropriate, using boundary and partition testing. Regression testing is considered. Tests are linked to acceptance criteria and scenarios. | Excellent and comprehensive testing is carried out. Unit tests thoroughly validate and verify functionality. Coverage is strong with boundary and partition tests. Regression testing is clear and consistent. Tests are explicitly linked to acceptance criteria and scenarios. |
| Planning | No burndown chart, definition of done, or sprint plan is presented. No evidence of progress tracking or reprioritisation. No task summary. | A basic burndown chart is presented but may be incorrect or incomplete. Definition of done is vague or inconsistently applied. Sprint planning is weak. Task summary is limited. | A satisfactory burndown chart is presented, showing some progress. A definition of done exists but lacks consistency. Sprint planning is present but limited in detail. Some reprioritisation is shown. A basic task summary is included. | A good burndown chart is presented in the correct format, showing clear progress. A clear definition of done exists and is followed. Sprint planning for the next increment is appropriate. Reprioritisation is managed effectively. Task summary table is clear. | An excellent burndown chart is presented in the correct format with detailed and accurate tracking. A strong and consistently applied definition of done exists. Sprint planning for the next increment is clear, detailed, and realistic. Reprioritisation is well justified. Task summary is detailed and accurate. |

### D7: Increment 3

**Deliverable Champion: Jian Shi**

Increment 3 is your final increment. It should proceed in the same way as Increments 1 and 2, with the implementation of the user stories that you have planned for the third sprint. You should continue to follow best practices for software development, including code quality, documentation, and testing.

In addition to the criteria for Increments 1 and 2, you should develop your practices as follows.

#### User Guide

You should produce a user guide for your application, that explains how to use it. It should be clear, concise and user-centred. It should contain all pertinent information, and no more.

#### Testing

- Expand your use of automated testing to verify correctness of the application.
- Develop and use objective acceptance criteria for the user stories that you are implementing, and test your application against them.

#### What to Submit

Please submit a <span class="update"> **PDF file, word file, or presentation deck** </span> , containing your project documentation. You should include any new or updated UML diagrams or other design artifacts that you have produced, and any project management artifacts that you have created, and evidence of your testing. You should include a link to your GitLab repository.

Also submit a **zip or tar archive** containing your Java application and any jar files or other dependencies that are necessary to run it. The application must compile and run without errors.

#### D7 Marking Rubric

| Category | 0–40 | 50 | 60 | 70 | 80+ |
|----|----|----|----|----|----|
| **Working Code** | Code does not run or is largely incorrect/unstable. No real value or usability. | Basic code runs but has limited correctness or robustness. Limited value and poor user experience. Only partially meets requirements. | Satisfactory code runs with some correctness and robustness. Provides some value and an adequate user experience. Generally meets requirements but not fully. | Good code runs correctly and robustly. Provides clear value, good user experience, and meets requirements as defined. | Excellent code is correct, robust, and reliable. Provides strong value, excellent user experience, and fully meets re-negotiated requirements. |
| **User Guide** | No user guide is presented, or it is irrelevant/incoherent. | A basic user guide is presented, but it lacks clarity, completeness, or user-centred focus. | A satisfactory user guide is presented, covering necessary points but with limited clarity or conciseness. | A good user guide is presented, clear and user-centred, including all pertinent information without unnecessary detail. | An excellent user guide is presented, with rich clarity, conciseness, and strong user focus. It contains exactly the needed information. |
| **Design and Planning** | No clear burndown/progress chart, no meaningful design decisions or artifacts, and no task summary. | Basic burndown/progress charts are presented but incomplete or unclear. Some design decisions are made but weakly justified. Few or incomplete artifacts. Task summary is limited. | Satisfactory burndown/progress charts are presented. Reasonable design decisions are made with some justification. Some artifacts are supplied. A basic task summary is included. | Good burndown/progress charts are presented and accurate. Design decisions are justified appropriately. Suitable design artifacts are provided in correct format. Task summary table is clear. | Excellent burndown/progress charts are presented and used effectively. Design decisions are well justified. Comprehensive and correct artifacts are supplied. Task summary is detailed and accurate. |
| **Testing** | No automated unit tests or testing evidence. No acceptance criteria tested. | Basic unit tests are presented, but coverage is weak. Limited evidence of other techniques. Few acceptance criteria tested. | Satisfactory automated unit tests are presented. Some additional techniques (e.g. integration or boundary tests) are used. Some acceptance criteria are tested. | Good automated unit tests are presented with clear evidence. A range of techniques (component, integration, boundary, regression) are used. Acceptance criteria and scenarios are tested. | Excellent and comprehensive automated unit tests are presented. A full range of testing techniques is applied systematically. All acceptance criteria and scenarios are thoroughly tested. |

### D8: Evaluation Report

**Deliverable Champion: Haiming Liu**

You must conduct a summative evaluation of your high-fidelity prototype, using the evaluation plan that you prepared in Deliverable 3. You may update your evaluation plan if necessary, but you should justify any changes that you make.

#### Evaluation Materials

You should prepare any materials that you need for your evaluation, including recruitment materials, consent forms, questionnaires, task instructions, post-task questionnaires, interview questions, etc.

#### Conducting the Evaluation

You should recruit participants for your evaluation, and conduct the evaluation according to your plan. You must recruit participants from another COMP2300 group, and you must conduct the evaluation in a lab setting (i.e. not remotely, and not in the wild; although this needn't be an actual lab - just a quiet room where you can control the environment).

#### Data Analysis

You should analyse the data that you collect, using appropriate analysis techniques. You may use both quantitative and qualitative analysis techniques as appropriate, and you should ensure that your analysis is rigorous and reliable. Your report should explain and justify the analysis techniques that you use, and present the results of your analysis clearly.

#### Discussion and Recommendations

You should discuss the implications of your findings, and make recommendations for future work. This may include suggestions for improving your prototype, and potentially ideas for further evaluation.

#### What to Submit

Please submit a single PDF file, containing your evaluation materials, a description of how you conducted the evaluation, a description and justification of your data analysis, the results of the analysis, a discussion of the results and recommendations.

> Don't forget to include the usual **[Group Deliverable Report](#section-15-Group%20Deliverable%20Reports)** as part of your submission.

#### D8 Marking Rubric

|  | 0-40 | 50 | 60 | 70 | 80+ |
|----|----|----|----|----|----|
| Evaluation Method & Materials | No evaluation method or materials are presented, or they are irrelevant or incoherent. | A basic evaluation method and materials are presented, but they may not clearly reflect a useful user-centred evaluation. | A satisfactory evaluation method and materials are presented. They generally reflect a somewhat useful user-centred evaluation linked to some relevant acceptance criteria. | A good evaluation method and materials are presented. They reflect a useful user-centred evaluation, with links to some relevant acceptance criteria. | An excellent evaluation method and materials are presented. They reflect a useful and reliable user-centred evaluation, thoroughly grounded in relevant acceptance criteria. |
| Analysis & Results | No analysis or results are presented, or they are irrelevant or incoherent. | Basic analysis and results are presented, which may not be wholly relevant to a user-centred evaluation. Application of analysis techniques is generally sound, but with some errors or inconsistency. | Satisfactory analysis and results are presented, relevant to the evaluation criteria. Sound application of relevant analysis techniques. | Good analysis and results are presented, with clear detail and strong relevance to the acceptance criteria. They accurately reflect the collected data. Good application of relevant analysis techniques. | Excellent analysis and results are presented, with rich detail and highly relevant to the acceptance criteria. They thoroughly reflect the data collected. Very good application of relevant analysis techniques. |
| Discussion & Recommendations | No discussion or recommendations are presented, or they are irrelevant or incoherent. | A basic discussion and recommendations are presented, but they may not be linked to the results of the evaluation. | A satisfactory discussion and recommendations are presented, with some links to the results of the evaluation. Some suggestions for future work are made. | A good discussion and recommendations are presented, with clear links to the results of the evaluation. Good suggestions for future work are made. | An excellent discussion and recommendations are presented, with strong links to the results of the evaluation. Insightful suggestions for future work are made. |

## Group Meetings

To make progress on this module, it is essential that you meet regularly as a group. Meetings should take place at least once per week, and you must submit a meeting record after each meeting using [groups.ecs.soton.ac.uk](https://groups.ecs.soton.ac.uk) .

### Meeting Basics

Everyone should be informed of the time, date and location of the each meeting in advance. Ideally, use the same slot each week (and choose a time that everyone can attend).

We expect that all members will attend all meetings. However, in the rare case that you cannot attend a meeting (for example because of illness or a personal emergency), you must inform the group in advance by giving your apologies. It is not necessary to give a detailed account of why you cannot attend. If circumstances mean that you routinely cannot attend at a particular time, the group should find an alternative time to meet.

The minutes of a meeting are a formal record. At each meeting, you should appoint a minute-taker to keep notes. Minutes should include actions - which are the tasks agreed by the group - as well as other notes about decisions that have been taken. Actions must be assigned to one or more people, who will be responsible for carrying the action out.

You should review actions from previous meetings at the start of each meeting (or during a relevant agenda item) to ensure that progress is being made. Actions should be updated regularly to reflect their current status, and either marked as completed or carried over to a future meeting.

### Submitting your meeting records

After each meeting, you must submit meeting records. Group members should collaborate to ensure that the minutes are accurate and complete before submission. Meeting records should be submitted using the [groups.soton.ac.uk](https://groups.soton.ac.uk) platform. If any member disagrees with the minutes, they should raise their concerns with the group before submission; but if they are not satisfied they may raise a dispute to the module team, using the appropriate option in the platform.

> ⚠️ Your meeting records will be available to your supervisor, and also to the module team. They will be used to inform your group and individual marks.

## Group Deliverable Reports

For each deliverable, groups must submit **a deliverable report** and any **associated artefacts** (e.g. designs, code, documentation) by the specified deadline. Consult the deliverable specifications for specific details of what you need to submit for each deliverable.

In addition to any deliverable-specific requirements, all your group deliverable reports must include:

- A summary of the work completed for the deliverable.
- A report on how you approached the specific tasks that make up the deliverable.
- A description of the project management techniques used to complete the deliverable. You may assume that a reader has read your previous deliverable reports, so you do not need to repeat information about your overall project management approach; focus on any changes or adaptations you made for this deliverable.
- A description of the tools and techniques used to complete the deliverable.
- A reflection on the process of completing the deliverable, including what went well and what could be improved in terms of product and process.

> In later deliverables, your sprint reviews and retrospectives will help you to complete your reflection on product and process.

You may find it helpful to include photographs, screenshots, or other visual materials to document and explain your work.

> **Remember:** We are assessing your ability to manage and deliver a project using appropriate techniques, not just the final product. Your reports should demonstrate your understanding of project management, design, and software engineering principles, and your ability to apply them in practice. Use your reports to show how you are approaching the project and applying what you have learned in the module.

## PRISM (40%)

In SDDP, as in life, you must build and maintain good working relationships with others. You must communicate effectively, collaborate, and contribute to the success of your team. Sometimes that will be easy, and sometimes it will be challenging. Your ability to work well with others is a crucial skill.

Your will receive a peer-review informed skills assessment (PRISM) mark, which will be based on a combination of peer-assessments from your group members, evidence of your individual contributions from GitLab, and observations made by your group supervisor.

The PRISM mark will reflect your overall performance in the group project, taking into account both your individual efforts and your collaboration with others. It is an assessment of your technical and teamworking skills. You will receive one PRISM mark at the end of Semester 1, and one at the end of Semester 2. Each PRISM mark will contribute 20% to your overall module mark.

> ⚠️ You do **not** need to take on the role of group leader to obtain a high PRISM mark; you can achieve this through active participation and high-quality contribution in any role.

### Minimum Expectations

We expect that all students on the module will:

- Attend all or most scheduled meetings, and provide apologies in advance on the rare occasion that they cannot.
- Actively participate in discussions.
- Complete assigned tasks on time and to a high standard.
- Communicate openly and respectfully with peers and supervisors and contribute to a positive team environment.
- Behave professionally and inclusively, making reasonable adjustments to accommodate different perspectives and needs.
- Seek feedback, reflect on their own strengths and weaknesses, and take action to improve.

### Peer Review Process

Peer reviews will be managed using the [groups.ecs.soton.ac.uk](https://groups.ecs.soton.ac.uk) interface. You **must** submit weekly feedback. Low engagement with peer reviews will negatively affect your own PRISM mark.

### PRISM Marking Rubric

| Mark | Minimum Expectations | Meeting Contributions | Technical Deliveries | Organisation | Communication | Initiative |
|----|----|----|----|----|----|----|
| **80** | Proactive, professional, and technically skilled contribution. Excellent collaboration and communication. Consistently exceeds expectations. **Consistently contributes to peer-review.** | Always attends and actively participates in meetings, providing valuable input and supporting decisions. | Consistently produces outstanding technical work, demonstrating expert application of appropriate design and software engineering skills, and contributes significantly to deliverables. | Exceptionally organised, efficiently manages tasks, and helps coordinate group activities. | Communicates clearly, respectfully, and ensures all information is shared and understood. | Proactively identifies improvements, volunteers for tasks, and suggests constructive solutions. |
| **70** | Strong contribution with good technical skills. Solid collaboration and communication. Meets and occasionally exceeds expectations. **Consistently contributes to peer-review.** | Regularly attends and participates in meetings, often provides useful input. | Produces high-quality technical work, showing good application of design and software engineering skills, and contributes well to deliverables. | Well organised, manages tasks effectively, and supports group coordination. | Communicates well and ensures information is generally shared and understood. | Often volunteers for tasks and suggests improvements. |
| **60** | Satisfactory contribution with basic technical skills. Adequate collaboration and communication. Usually meets minimum expectations. **Consistently contributes to peer-review.** | Attends most meetings and participates when prompted, provides some input. | Produces acceptable technical work, demonstrating basic application of design and software engineering skills, and contributes to deliverables. | Adequately organised, manages own tasks, and occasionally helps with group activities. | Communicates sufficiently, but may miss sharing some information. | Sometimes volunteers or suggests improvements. |
| **50** | Limited contribution with gaps in technical skills and collaboration. Fails to meet expectations. **Contributes to the majority of peer-reviews.** | Infrequently attends or participates in meetings, rarely provides input. Possibly occasional unprofessional behaviour. | Technical work is incomplete or of low quality, with limited application of design and software engineering skills. | Poor organisation, struggles to manage tasks, rarely helps coordinate. | Communication is inconsistent or unclear, information is often not shared. | Rarely volunteers or suggests improvements. |
| **40** | Poor contribution with little evidence of technical skills or collaboration. Fails to meet expectations. **Contributes to peer-review sometimes.** | Rarely attends meetings, does not participate or support group decisions, or behaves unprofessionally. | Little or no technical work produced, with minimal or no application of design and software engineering skills. | Disorganised, does not manage tasks or contribute to group activities. | Communication is poor or absent, information not shared. | Does not volunteer or suggest improvements. |
| **\< 40** | Negligible contribution, no evidence of technical skills or collaboration. Significantly fails to meet expectations. **Little or no engagement with peer-review.** | Does not attend meetings or participate in any way. | No technical work produced, no evidence of design or software engineering skills. | No organisation or task management. | No communication with group members. | No initiative shown at any stage. |

## The Individual Reflective Reports (10%)

Alongside each group deliverable, you must each individually submit a short reflective report about your contributions to the group project, the process that our group has followed, and the skills and theory that you have applied. The aim of the statement is to reflect critically on your progress and demonstrate your understanding of the relevant skills and knowledge.

The reports that you submit will be compiled and marked in two stages; once in December, and once at the end of Semester 2. You will receive formative feedback after the December deadline to help you improve your subsequent reports.

For each report, you should comment on:

- What your contribution to the group project has been, and how\* it has impacted the overall outcome.
- The process that your group has followed, explained using appropriate terms and theory from design and software engineering project management, and detailing how techniques from the skill sessions were incoporated.
- Areas for improvement in your own contributions and the group process, with suggestions about how to implement those improvements based on relevant theory.
- Evaluation of previous changes you have made as an individual or group.

You must link your report with what has been covered in lectures, in the skills workshops, and in your own background reading. You should refer to the design processes, project management techniques, and any other material that you think relevant. You should think about what worked or did not work effectively in your team, and how you influenced (or could have influenced) that. For this report, we would like you to think about your own future practice, and we do not want you to reflect on how COMP2300 should be run in future.

> (\* by *'how'* we mean *'in what ways'* or *'through what mechanisms'* your contribution has influenced the project, not merely *'degree of contribution'* or *'success of contribution'* )

**Each report may be up to <span class="update">300 words long</span>** for a total of 2400 words across the eight deliverables, or 1200 words per semester.

### What to Submit

Individual reports must be submitted as **markdown** files, using the assignment submission links in Moodle. You may include images or diagram as appendices, if you wish. If you submit images, please upload a zip file that contains the markdown file and the images.

### Individual Reflective Report Marking Criteria

| Mark Range | Clarity | Insight | Theory & Techniques |
|----|----|----|----|
| 80+ | Exceptionally precise, well-structured, and concise writing. Every statement is meaningful and directly advances the argument. All aspects are thoroughly and insightfully addressed. Arguments are exceptionally clear and compelling. | Deep, critical, and original reflection on both individual and group progress, with highly insightful and well-justified suggestions for improvement. Demonstrates outstanding self-appraisal and group analysis. | Consistently and expertly applies relevant theory and techniques from software development, design, and project management. Theory and experiences from skills sessions are seamlessly integrated and used to provide strong support for arguments. |
| 70-79 | Very precise, well-structured, and concise writing. All statements are meaningful and contribute directly to the argument. All requested aspects are thoroughly covered and clearly addressed. Arguments are easy to follow. | Deep, critical reflection on individual and group progress, with well-justified suggestions for improvement. Demonstrates sophisticated self-appraisal and group analysis. | Consistently and accurately applies relevant theory and techniques from software development, design, and project management. Theory and experiences from skills sessions are well integrated to support arguments. |
| 60-69 | Clear and well-organized writing with a high level of precision. Most statements are meaningful and relevant. Arguments are logical and coherent. | Thoughtful reflection with some critical appraisal and relevant suggestions for improvement. Good self-awareness and group analysis. | Relevant theory and techniques are applied appropriately and support most arguments. Some integration of theory into reflection. |
| 50-59 | Generally clear and understandable, though some statements may lack precision or relevance. Structure is mostly appropriate. | Some reflection and appraisal, with basic suggestions for improvement. Limited depth in analysis. | Some relevant theory and techniques are referenced, though application may be superficial or incomplete. |
| 40-49 | Writing is sometimes unclear or lacks precision. Some statements may be ambiguous or not meaningful. Arguments may be hard to follow. | Limited reflection or critical appraisal. Suggestions for improvement are vague or unsupported. | Minimal or incorrect reference to relevant theory. Application is weak or not well connected to arguments. |
| 0-39 | Writing is unclear, imprecise, and difficult to understand. Most statements lack meaning or relevance. | Little or no reflection or critical appraisal. No meaningful suggestions for improvement. | No relevant theory referenced or applied. |

## Presentation Guidelines and Referencing

The most important thing is that all your submitted work is legible, easy to follow, and provides sufficient evidence of your work and thinking to allow us to award you marks. However, there are some requirements we insist upon. Please make sure that each of your submissions meets the following guidelines.

### PDF Formatting

When submitting your work as a PDF, please use a sensible font, of a size no less than 12pt. Pages should be formatted to A4 size, with margins of at least 1.5cm. Use headings, subheadings, paragraphs and bullet points appropriately, to create a clear and logical document.

### PDF Title Pages

Each group hand-in should have a title page that sets out:

- The course (COMP2300)
- The title of the task/hand-in
- The date and version of the document (in case you later submit an update)
- The name and email address of the authors

### Markdown

Your individual reflection reports must be written in Markdown. You should use headings, subheadings, paragraphs and bullet points appropriately, to create a clear and logical document. You may use other Markdown features (e.g. tables, images, links) as appropriate. We will use a Github-flavoured Markdown renderer to view your reports, so you may wish to refer to the GitHub documentation for details of the supported features: <https://docs.github.com/en/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax>

### Citations

You must cite your sources properly, using the ACM SIGCHI referencing style. Each hand-in where you have cited sources should have its own references section at the end of the document.

Other referencing systems are not allowed and must not be used. You may wish to use a reference manager such as Zotero to help format references, but you must remember to check that it has correctly extracted the metadata from the articles and that the references it generates are correct!

You are reminded that failing to properly acknowledge the source of information, or to identify what is your own work and what is not, may be a violation of the University’s academic integrity policy ( <https://www.southampton.ac.uk/quality/assessment/academic_integrity.page> ).

Reference lists should be included at the **end** of your document, prior to any appendices. You must not use footnotes for citations.

### Word Limits

Where word (or video length) limits are set, we will not mark content beyond the limit.

<div id="schedule" class="section">

## Schedule and Deliverables

This table shows everything that you need to submit for the module. You may wish to print this table and check the tasks off as they are completed!

|  | Group |  | Individual |  |
|----|----|----|----|----|
| Week | Moodle | groups.ecs | Moodle | groups.ecs |
| Week 3 2025-10-13 |  | Week 3 meeting notes |  | Week 3 peer review |
| Week 4 2025-10-20 |  | Week 4 meeting notes |  | Week 4 peer review |
| Week 5 2025-10-27 | D1 (Friday 16:00) | Week 5 meeting notes | D1 individual report (Friday 17:00) | D1 peer review |
| Week 6 2025-11-03 |  | Week 6 meeting notes |  | Week 6 peer review |
| Week 7 2025-11-10 | D2 (Friday 16:00) | Week 7 meeting notes | D2 individual report (Friday 17:00) | D2 peer review |
| Week 8 2025-11-17 |  | Week 8 meeting notes |  | Week 8 peer review |
| Week 9 2025-11-24 | D3 (Friday 16:00) | Week 9 meeting notes | D3 individual report (Friday 17:00) | D3 peer review |
| Week 10 2025-12-01 |  | Week 10 meeting notes |  | Week 10 peer review |
| Week 11 2025-12-08 | D4 (Friday 16:00) | Week 11 meeting notes | D4 individual report (Friday 17:00) | D4 peer review |
| Week 15 2026-01-05 |  | Week 15 meeting notes |  | Week 15 peer review |
| Week 19 2026-02-02 |  | Week 19 meeting notes |  | Week 19 peer review |
| Week 20 2026-02-09 | D5 (Friday 16:00) | Week 20 meeting notes | D5 individual report (Friday 17:00) | D5 peer review |
| Week 21 2026-02-16 |  | Week 21 meeting notes |  | Week 21 peer review |
| Week 22 2026-02-23 |  | Week 22 meeting notes |  | Week 22 peer review |
| Week 23 2026-03-02 | D6 (Friday 16:00) | Week 23 meeting notes | D6 individual report (Friday 17:00) | D6 peer review |
| Week 24 2026-03-09 |  | Week 24 meeting notes |  | Week 24 peer review |
| Week 25 2026-03-16 |  | Week 25 meeting notes |  | Week 25 peer review |
| Week 30 2026-04-20 | D7 (Friday 16:00) | Week 30 meeting notes | D7 individual report (Friday 17:00) | D7 peer review |
| Week 31 2026-04-27 |  | Week 31 meeting notes |  | Week 31 peer review |
| Week 32 2026-05-04 |  | Week 32 meeting notes |  | Week 32 peer review |
| Week 33 2026-05-11 | D8 (Friday 16:00) | Week 33 meeting notes | D8 individual report (Friday 17:00) | D8 peer review |

</div>
